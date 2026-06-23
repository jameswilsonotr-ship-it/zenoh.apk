package com.example

import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ZenohViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ZenohAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ZenohAppScreen(
    viewModel: ZenohViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isJniAvailable by viewModel.isJniAvailable.collectAsState()
    val isHeartbeatRunning by viewModel.isHeartbeatRunning.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val debugLogs by viewModel.debugLogs.collectAsState()
    val stats by viewModel.diagnostics.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Bus Explorer, 1: Developer Console

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Status Dashboard Header
        HeaderDashboard(
            connectionState = connectionState,
            stats = stats,
            isJniAvailable = isJniAvailable,
            isHeartbeatRunning = isHeartbeatRunning
        )

        // 2. Tab Selectors
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Bus Explorer", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Search, contentDescription = null) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Dev Mode", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.DeveloperMode, contentDescription = null) }
            )
        }

        // Error message banner
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Tab Contents
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeTab) {
                0 -> BusExplorerTab(
                    viewModel = viewModel,
                    connectionState = connectionState,
                    isHeartbeatRunning = isHeartbeatRunning,
                    logs = logs
                )
                1 -> AdvancedDeveloperTab(
                    viewModel = viewModel,
                    isJniAvailable = isJniAvailable,
                    debugLogs = debugLogs,
                    stats = stats
                )
            }
        }
    }
}

@Composable
fun HeaderDashboard(
    connectionState: ConnectionState,
    stats: DiagnosticInfo,
    isJniAvailable: Boolean,
    isHeartbeatRunning: Boolean
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val stateColor = when (connectionState) {
        ConnectionState.CONNECTED -> Color(0xFF81C784) // Neon green
        ConnectionState.CONNECTING -> Color(0xFFFFB74D) // Neon orange
        ConnectionState.ERROR -> Color(0xFFEF5350) // Neon red
        ConnectionState.DISCONNECTED -> Color(0xFF919194) // Muted slate gray
    }

    val stateText = when (connectionState) {
        ConnectionState.CONNECTED -> "ON SYSTEM BUS"
        ConnectionState.CONNECTING -> "NEGOTIATING..."
        ConnectionState.ERROR -> "PROTOCOL ERROR"
        ConnectionState.DISCONNECTED -> "OFFLINE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Heartbeat state circle animation
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(stateColor)
                        .align(Alignment.CenterVertically)
                ) {
                    if (connectionState == ConnectionState.CONNECTING || isHeartbeatRunning) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(stateColor.copy(alpha = 0.4f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zenoh Swarm Node",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stateText,
                        color = stateColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                if (!isJniAvailable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFB300))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SIMULATED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3E2723)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "NATIVE JNI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Traffic Counter Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CounterElement(
                    title = "Heartbeats/Sent",
                    count = stats.totalSent.toString(),
                    icon = Icons.Default.ArrowUpward,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(35.dp)
                        .padding(horizontal = 12.dp)
                )
                CounterElement(
                    title = "Messages/Recv",
                    count = stats.totalReceived.toString(),
                    icon = Icons.Default.ArrowDownward,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier
                        .height(35.dp)
                        .padding(horizontal = 12.dp)
                )
                CounterElement(
                    title = "Throughput Rate",
                    count = String.format(Locale.US, "%.1f p/s", stats.throughputRate),
                    icon = Icons.Default.Timeline,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CounterElement(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusExplorerTab(
    viewModel: ZenohViewModel,
    connectionState: ConnectionState,
    isHeartbeatRunning: Boolean,
    logs: List<LogEntry>
) {
    var isExpandedConnect by remember { mutableStateOf(true) }
    var isExpandedPublisher by remember { mutableStateOf(false) }

    val routerEndpoint by viewModel.routerEndpoint.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val topicPrefix by viewModel.topicPrefix.collectAsState()
    val publishTopic by viewModel.publishTopic.collectAsState()
    val publishPayload by viewModel.publishPayload.collectAsState()
    val subscribeExpr by viewModel.subscribeExpr.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Connection Plate
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpandedConnect = !isExpandedConnect },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SettingsEthernet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Connection Configurations",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (isExpandedConnect) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Section"
                        )
                    }

                    if (isExpandedConnect) {
                        Spacer(modifier = Modifier.height(12.dp))

                        TextField(
                            value = routerEndpoint,
                            onValueChange = { viewModel.routerEndpoint.value = it },
                            label = { Text("Zenoh Target Endpoint") },
                            placeholder = { Text("tcp/192.168.1.100:7447") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("router_endpoint"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented buttons for Mode Selection (Client / Peer)
                        Text(
                            text = "Orchestration Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.mode.value = "client" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (mode == "client") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (mode == "client") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Client (Routed)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.mode.value = "peer" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (mode == "peer") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (mode == "peer") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Peer (P2P Mesh)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Connection CTA Button
                        val buttonText = when (connectionState) {
                            ConnectionState.CONNECTED -> "Disconnect Node"
                            ConnectionState.CONNECTING -> "Negotiating Sync..."
                            else -> "Start System Bus Connection"
                        }

                        Button(
                            onClick = { viewModel.toggleConnect() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("connect_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectionState == ConnectionState.CONNECTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (connectionState == ConnectionState.CONNECTING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        if (connectionState == ConnectionState.CONNECTED) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(buttonText, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Swarm Diagnostics or Subscribers Action
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.RssFeed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bus Subsciption Path",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = subscribeExpr,
                            onValueChange = { viewModel.subscribeExpr.value = it },
                            placeholder = { Text("swarm/bus/**") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("subscribe_expr"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.declareSubscription(subscribeExpr) },
                            enabled = connectionState == ConnectionState.CONNECTED,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Subscribe", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Simple Publishers and Heartbeater card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpandedPublisher = !isExpandedPublisher },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manual Telemetry Publisher",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (isExpandedPublisher) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Section"
                        )
                    }

                    if (isExpandedPublisher) {
                        Spacer(modifier = Modifier.height(12.dp))

                        TextField(
                            value = publishTopic,
                            onValueChange = { viewModel.publishTopic.value = it },
                            label = { Text("Topic Path KeyExpr") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = publishPayload,
                            onValueChange = { viewModel.publishPayload.value = it },
                            label = { Text("Payload String (Text or JSON)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.publishMessage(publishTopic, publishPayload) },
                            enabled = connectionState == ConnectionState.CONNECTED,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Publish, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publish Payload", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. Autopilot Swarm Heartbeat Panel (One Tap)
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isHeartbeatRunning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isHeartbeatRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start System Bus Daemon",
                            fontWeight = FontWeight.Bold,
                            color = if (isHeartbeatRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Auto-subscribes to telemetry wildcard & publishes simulated heartbeat packet every 5s.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Switch(
                        checked = isHeartbeatRunning,
                        onCheckedChange = { viewModel.toggleHeartbeat() },
                        enabled = connectionState == ConnectionState.CONNECTED,
                        modifier = Modifier.testTag("heartbeat_switch")
                    )
                }
            }
        }

        // 5. Subscriber Live Log Terminal Stream
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zenoh Bus Stream Console",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                TextButton(onClick = { viewModel.clearLogs() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Stream", fontSize = 12.sp)
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No telemetry packages received yet",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                "Connect and subscribe to see active bus messages",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { item ->
                ConsoleRowEntry(item)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ConsoleRowEntry(entry: LogEntry) {
    val clipManager = LocalClipboardManager.current

    val prefixColor = if (entry.isSystem) Color(0xFFFFB74D) // Vivid orange
    else if (entry.isSent) Color(0xFFD0BCFF) // Purple80 primary color
    else Color(0xFF81C784) // Vivid green

    val labelText = if (entry.isSystem) "SYSTEM"
    else if (entry.isSent) "PUBLISHED"
    else "RECEIVED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                clipManager.setText(AnnotatedString("[${entry.timestamp}] ${entry.topic}: ${entry.payload}"))
            },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF000000) // Pure black block
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, prefixColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(prefixColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = labelText,
                        color = prefixColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = entry.timestamp,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF919194)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.topic,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (entry.isSystem) Color(0xFFFFB74D) else prefixColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.payload,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFE2E2E6),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun AdvancedDeveloperTab(
    viewModel: ZenohViewModel,
    isJniAvailable: Boolean,
    debugLogs: List<String>,
    stats: DiagnosticInfo
) {
    val qosReliable by viewModel.isQosReliable.collectAsState()
    val traceEnabled by viewModel.isTraceEnabled.collectAsState()
    val heartbeatInterval by viewModel.heartbeatInterval.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Diagnostic Summary Table
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Diagnostics & Spec Table",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    DiagnosticsRow(label = "Session UUID identifier", value = stats.sessionId)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    DiagnosticsRow(label = "Active Router Mode", value = stats.mode)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    DiagnosticsRow(label = "Rust JNI Binary Core Status", value = stats.jniStatus)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    DiagnosticsRow(
                        label = "Uptime duration statistics",
                        value = "${stats.uptimeMs / 1000L} seconds"
                    )
                }
            }
        }

        // 2. Settings list
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Advanced Protocol Configuration",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // QoS Select Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Best-Effort vs Reliable QoS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Forces transport protocol to request receipt acknowledgments.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = qosReliable,
                            onCheckedChange = { viewModel.isQosReliable.value = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Trace switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trace Diagnostics Logging", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Outputs verbose JNI wrapper symbols and internal event states.", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = traceEnabled,
                            onCheckedChange = { viewModel.isTraceEnabled.value = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Heartbeat interval customization
                    Column {
                        Text("Simulated Node Heartbeat Period", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Interval: $heartbeatInterval seconds", fontSize = 12.sp, color = Color.Gray)
                        Slider(
                            value = heartbeatInterval.toFloat(),
                            onValueChange = { viewModel.heartbeatInterval.value = it.toInt() },
                            valueRange = 2f..30f,
                            steps = 14
                        )
                    }
                }
            }
        }

        // 3. JNI System Info
        item {
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isJniAvailable) MaterialTheme.colorScheme.surface
                    else Color(0xFF33230A),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isJniAvailable) MaterialTheme.colorScheme.outline
                    else Color(0xFFFFB74D).copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isJniAvailable) "JNI Environment Nominal" else "UnsatisfiedLinkError Resolution Guide",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isJniAvailable) MaterialTheme.colorScheme.onSurface else Color(0xFFFFB74D)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isJniAvailable) {
                            "The precompiled Zenoh peer-to-peer Rust library has successfully linked into your Android app's process space. Sockets can be dynamically spawned."
                        } else {
                            "Zenoh's precompiled native C/Rust .so structures were skipped for this CPU architecture or emulator level. The app has enabled automatic local loopback simulation so you can explore topic paths, sliders, and telemetry logs."
                        },
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 4. Debug Logs Title Block
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Internal JNI / API Log Traces",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                TextButton(onClick = { viewModel.clearDebugLogs() }) {
                    Text("Clear Traces", fontSize = 12.sp)
                }
            }
        }

        // 5. Raw Trace Log Items
        if (debugLogs.isEmpty()) {
            item {
                Text(
                    text = "Log buffer is empty.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        } else {
            items(debugLogs.reversed()) { str ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = str,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
