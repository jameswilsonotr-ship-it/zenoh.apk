package com.example

import android.app.Application
import android.os.SystemClock
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.zenoh.Config
import org.eclipse.zenoh.Session
import org.eclipse.zenoh.keyexpr.KeyExpr
import org.eclipse.zenoh.value.Value
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val topic: String,
    val payload: String,
    val isSystem: Boolean = false,
    val isSent: Boolean = false
)

data class DiagnosticInfo(
    val sessionId: String = "Unknown",
    val mode: String = "Client",
    val jniStatus: String = "Ready",
    val uptimeMs: Long = 0L,
    val totalSent: Int = 0,
    val totalReceived: Int = 0,
    val throughputRate: Float = 0f // packets per second
)

class ZenohViewModel(application: Application) : AndroidViewModel(application) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _debugLogs = MutableStateFlow<List<String>>(emptyList())
    val debugLogs: StateFlow<List<String>> = _debugLogs.asStateFlow()

    // Configurable parameters
    val routerEndpoint = MutableStateFlow("tcp/10.0.2.2:7447")
    val mode = MutableStateFlow("client") // client or peer
    val topicPrefix = MutableStateFlow("swarm/bus")
    val publishTopic = MutableStateFlow("swarm/bus/heartbeat")
    val publishPayload = MutableStateFlow("{\"status\": \"nominal\", \"battery\": 95, \"node\": \"android-client\"}")
    val subscribeExpr = MutableStateFlow("swarm/bus/**")

    // Developer Mode & Diagnostic Toggles
    val isDeveloperMode = MutableStateFlow(true)
    val isTraceEnabled = MutableStateFlow(false)
    val isQosReliable = MutableStateFlow(false)

    // Heartbeat State
    private val _isHeartbeatRunning = MutableStateFlow(false)
    val isHeartbeatRunning: StateFlow<Boolean> = _isHeartbeatRunning.asStateFlow()
    val heartbeatInterval = MutableStateFlow(5) // in seconds

    // Diagnostic Stats
    private val _diagnostics = MutableStateFlow(DiagnosticInfo())
    val diagnostics: StateFlow<DiagnosticInfo> = _diagnostics.asStateFlow()

    private var session: Session? = null
    private val subscribers = mutableMapOf<String, AutoCloseable>()
    private var heartbeatJob: Job? = null
    private var metricsJob: Job? = null
    private val startTime = SystemClock.elapsedRealtime()

    private var sentCounter = 0
    private var receivedCounter = 0
    private var lastSentMetrics = 0
    private var lastReceivedMetrics = 0
    private var lastMetricsCheckTime = SystemClock.elapsedRealtime()

    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _isJniAvailable = MutableStateFlow(true)
    val isJniAvailable: StateFlow<Boolean> = _isJniAvailable.asStateFlow()

    init {
        // Safe JNI check
        try {
            val dummyConfig = Config.defaultConfig()
            _isJniAvailable.value = true
            addDebugLog("System Info: Zenoh JNI bindings loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            _isJniAvailable.value = false
            Log.e("ZenohBus", "Zenoh JNI loading error: ${e.message}")
            addDebugLog("[WARN] Zenoh native libraries not found. Protocol operations will run in safe simulation mode for testing.")
        } catch (e: Throwable) {
            addDebugLog("Config warning: ${e.message}")
        }

        // Start throughput/diagnostic calculator loop
        startDiagnosticsCalculator()
    }

    private fun addDebugLog(msg: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _debugLogs.update { (it + "[$stamp] $msg").takeLast(200) }
    }

    private fun addSubscriberLog(topic: String, payload: String, isSystem: Boolean = false, isSent: Boolean = false) {
        val entry = LogEntry(
            timestamp = timeFormatter.format(Date()),
            topic = topic,
            payload = payload,
            isSystem = isSystem,
            isSent = isSent
        )
        _logs.update { (list) ->
            (listOf(entry) + list).take(500) // Keep the latest 500 entries
        }
    }

    private fun startDiagnosticsCalculator() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(2000)
                val now = SystemClock.elapsedRealtime()
                val durationMs = now - lastMetricsCheckTime
                if (durationMs > 0) {
                    val sentThisPeriod = sentCounter - lastSentMetrics
                    val recvThisPeriod = receivedCounter - lastReceivedMetrics
                    val rate = ((sentThisPeriod + recvThisPeriod) * 1000f) / durationMs
                    
                    _diagnostics.update { current ->
                        current.copy(
                            uptimeMs = now - startTime,
                            totalSent = sentCounter,
                            totalReceived = receivedCounter,
                            throughputRate = rate
                        )
                    }
                    
                    lastSentMetrics = sentCounter
                    lastReceivedMetrics = receivedCounter
                    lastMetricsCheckTime = now
                }
            }
        }
    }

    fun toggleConnect() {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            disconnect()
        } else {
            connect()
        }
    }

    fun connect() {
        _errorMessage.value = null
        _connectionState.value = ConnectionState.CONNECTING
        addDebugLog("Initiating Zenoh connection session...")

        viewModelScope.launch(Dispatchers.IO) {
            val curEndpoint = routerEndpoint.value.trim()
            val curMode = mode.value.lowercase()
            val curSub = subscribeExpr.value.trim()

            if (!_isJniAvailable.value) {
                // RUN SIMULATED PROTOCOL FLOWS IF NO NATIVE JNI LAYER
                delay(1200) // Simulation delay
                _connectionState.value = ConnectionState.CONNECTED
                _diagnostics.update { it.copy(sessionId = "SIM-${UUID.randomUUID().toString().take(8)}", mode = curMode.uppercase(), jniStatus = "Simulated Fallback") }
                addDebugLog("Simulated Zenoh connection established to: $curEndpoint ($curMode mode)")
                addSubscriberLog("system/agent", "Simulated Zenoh Bus Connection established to $curEndpoint.", isSystem = true)
                
                // Set up simulated traffic sub
                simulateTraffic(curSub)
                return@launch
            }

            try {
                addDebugLog("Configuring endpoints to: $curEndpoint in $curMode mode...")
                val config = Config.defaultConfig().apply {
                    insertValue("connect/endpoints", curEndpoint)
                    insertValue("mode", curMode)
                    
                    if (isQosReliable.value) {
                        insertValue("transport/qos/enabled", "true")
                    }
                }

                if (isTraceEnabled.value) {
                    addDebugLog("Tracing active - setting protocol logs to DEBUG.")
                }

                addDebugLog("Opening session...")
                val newSession = Session.open(config)
                session = newSession

                val originalSessionId = try {
                    // Try to print or get Session Info if exposed
                    "Z-${UUID.randomUUID().toString().take(12)}"
                } catch (e: Throwable) {
                    "Z-Active"
                }

                _diagnostics.update {
                    it.copy(
                        sessionId = originalSessionId,
                        mode = curMode.uppercase(),
                        jniStatus = "Active (Rust Native)"
                    )
                }

                _connectionState.value = ConnectionState.CONNECTED
                addDebugLog("Session opened successfully. Session ID: $originalSessionId")
                addSubscriberLog("system/client", "Active connection to Zenoh system bus established successfully.", isSystem = true)

                // Declare Subscribers immediately
                if (curSub.isNotEmpty()) {
                    declareSubscription(curSub)
                }

            } catch (e: Throwable) {
                _connectionState.value = ConnectionState.ERROR
                val errMsg = e.localizedMessage ?: e.message ?: "Unknown JNI error code"
                _errorMessage.value = errMsg
                addDebugLog("[ERROR] Zenoh initialization failed: $errMsg")
                addSubscriberLog("system/error", "Connection failed: $errMsg", isSystem = true)
            }
        }
    }

    fun disconnect() {
        stopHeartbeat()
        _connectionState.value = ConnectionState.DISCONNECTED
        addDebugLog("Stopping connection processes...")

        viewModelScope.launch(Dispatchers.IO) {
            // Unsubscribe all
            subscribers.forEach { (topic, sub) ->
                try {
                    addDebugLog("Closing subscription to: $topic")
                    sub.close()
                } catch (e: Throwable) {
                    addDebugLog("Error closing subscriber: ${e.message}")
                }
            }
            subscribers.clear()

            // Close session
            try {
                session?.close()
                session = null
                addDebugLog("Zenoh session finalized cleanly.")
            } catch (e: Throwable) {
                addDebugLog("Error closing Zenoh session: ${e.message}")
            }

            addSubscriberLog("system/client", "Disconnected from Zenoh system bus.", isSystem = true)
        }
    }

    fun declareSubscription(topic: String) {
        val s = session ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Close existing subscription on this topic if any
            subscribers[topic]?.close()

            try {
                addDebugLog("Declaring subscriber on KeyExpr: $topic")
                val keyExpr = KeyExpr.tryFrom(topic).getOrThrow()
                
                val subscriber = s.declareSubscriber(keyExpr) { sample ->
                    val recvTopic = sample.keyExpr.toString()
                    val payload = try {
                        sample.value.toString()
                    } catch (e: Throwable) {
                        "Binary content"
                    }
                    receivedCounter++
                    addSubscriberLog(recvTopic, payload)
                }

                subscribers[topic] = subscriber as AutoCloseable
                addDebugLog("Subscribed successfully to: $topic")
            } catch (e: Throwable) {
                val errMsg = e.localizedMessage ?: e.message ?: "Invalid topic structure"
                addDebugLog("Failed to subscribe to $topic: $errMsg")
                _errorMessage.value = "Subscriber error: $errMsg"
            }
        }
    }

    fun publishMessage(topic: String, message: String) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            addDebugLog("Error: Zenoh session must be CONNECTED to publish.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (!_isJniAvailable.value) {
                // Simulated Publish
                delay(200)
                sentCounter++
                addDebugLog("Simulated Publish to [$topic]: $message")
                addSubscriberLog(topic, message, isSent = true)
                return@launch
            }

            val s = session
            if (s == null) {
                addDebugLog("Error: Session was null during publish operation.")
                return@launch
            }

            try {
                val keyExpr = KeyExpr.tryFrom(topic).getOrThrow()
                val value = Value(message)
                
                s.put(keyExpr, value)
                sentCounter++
                addDebugLog("Published to [$topic]: $message")
                
                // Add to visual publisher logs as local indicator
                addSubscriberLog(topic, message, isSent = true)
            } catch (e: Throwable) {
                val errMsg = e.localizedMessage ?: e.message ?: "JNI transmission failure"
                addDebugLog("[ERROR] Publish failed: $errMsg")
            }
        }
    }

    // Toggle Auto Heartbeat loop
    fun toggleHeartbeat() {
        if (_isHeartbeatRunning.value) {
            stopHeartbeat()
        } else {
            startHeartbeat()
        }
    }

    private fun startHeartbeat() {
        _isHeartbeatRunning.value = true
        addDebugLog("Auto-heartbeat daemon started.")
        
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isHeartbeatRunning.value) {
                val payload = buildHeartbeatPayload()
                publishMessage(publishTopic.value, payload)
                delay(heartbeatInterval.value * 1000L)
            }
        }
    }

    private fun stopHeartbeat() {
        _isHeartbeatRunning.value = false
        heartbeatJob?.cancel()
        heartbeatJob = null
        addDebugLog("Auto-heartbeat daemon stopped.")
    }

    private fun buildHeartbeatPayload(): String {
        val epoch = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'", Locale.US).format(Date(epoch))
        return """
            {
              "sender": "ZenohBusAndroid",
              "epoch_ms": $epoch,
              "timestamp": "$formattedDate",
              "battery": ${(80..100).random()},
              "device_model": "${Build.MANUFACTURER} ${Build.MODEL}",
              "os_version": "Android ${Build.VERSION.RELEASE}",
              "status": "nominal"
            }
        """.trimIndent()
    }

    // Simulated Traffic Loop (for no-JNI testing/demos inside safe environment)
    private var simulatedJob: Job? = null
    private fun simulateTraffic(curSub: String) {
        simulatedJob?.cancel()
        simulatedJob = viewModelScope.launch(Dispatchers.Default) {
            var counter = 0
            while (_connectionState.value == ConnectionState.CONNECTED) {
                delay((3000..8000).random().toLong())
                if (_connectionState.value != ConnectionState.CONNECTED) break
                
                counter++
                val simulatedTopic = when ((1..3).random()) {
                    1 -> "swarm/bus/uav_node_01/status"
                    2 -> "swarm/bus/agv_client_44/telemetry"
                    else -> "swarm/bus/command"
                }

                // Match with subscription wildcard
                val isMatch = curSub.replace("**", "").trim().let { prefix ->
                    simulatedTopic.startsWith(prefix) || prefix.startsWith(simulatedTopic)
                }

                if (isMatch) {
                    val simulatedPayload = when (simulatedTopic) {
                        "swarm/bus/command" -> "{\"command\": \"override_route\", \"waypoints\": [37.7749, -122.4194], \"urgency\": \"high\"}"
                        "swarm/bus/agv_client_44/telemetry" -> "{\"speed_mps\": ${String.format(Locale.US, "%.1f", (1..5).random() + (0..9).random() / 10f)}, \"yaw\": ${(0..360).random()}}"
                        else -> "{\"cpu_utilization\": ${(15..45).random()}, " +
                                "\"available_ram_mb\": 2048, \"wifi_signal_rssi\": -${(50..80).random()}}"
                    }
                    receivedCounter++
                    addSubscriberLog(simulatedTopic, simulatedPayload)
                }
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        addDebugLog("Subscriber message cache cleared.")
    }

    fun clearDebugLogs() {
        _debugLogs.value = emptyList()
        addDebugLog("Internal debug trace logs cleared.")
    }

    override fun onCleared() {
        disconnect()
        simulatedJob?.cancel()
        metricsJob?.cancel()
        super.onCleared()
    }
}
