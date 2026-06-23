# Zenoh Swarm Bus Client (`ZenohBusClient`)

An enterprise-ready, zero-overhead pub/sub client application for Android, built on **Eclipse Zenoh** (`zenoh-kotlin-android:1.1.0`), modern Kotlin/Coroutines, and Jetpack Compose (Material 3).

This application serves as a developer explorer, terminal debugging cockpit, and sovereign system bus hub for **Iron Pearl / swarm orchestration** nodes (UAVs, mobile nodes, AGVs, and IoT clusters).

---

## 💻 Tech Stack & Architecture

- **Protocol Core**: Eclipse Zenoh Protocol, utilizing high-performance Rust bindings embedded via JNI.
- **Language**: 100% Kotlin with reactive Kotlin Coroutines & `MutableStateFlow` streams.
- **UI Framework**: Modern Jetpack Compose, designed around Material Design 3 guidelines.
- **Minimum SDK Target**: **30** (Required by JNI native POSIX and Rust runtime layers).
- **Permissions**:
  - `android.permission.INTERNET` (Socket layer)
  - `android.permission.ACCESS_NETWORK_STATE` (Dynamic interface monitoring)

---

## 🚀 Key Features

1. **System Bus Connection Control**: Connect in either **Client (Routed) Mode** (for router/broker topology) or **Peer (P2P Mesh) Mode** (for localized peer-to-peer swarm interaction).
2. **Terminal Console log**: Visual monospace streaming log of received telemetry packets or command events matching wildcard configurations (e.g., `swarm/bus/**`).
3. **Pulsing Swarm Indicators**: Animated visual signals displaying the node's heartbeat, live packet-per-second throughput statistics, and JNI library registration states.
4. **Autonomous Daemon Swarm Client (One Tap)**: Instantly starts a background heartbeat coroutine that auto-subscribes to telemetry topics and publishes structured JSON node statistics periodically.
5. **Interactive Telemetry Publisher**: Input block that allows broadcasting manual text/JSON strings to chosen Key Expressions.
6. **Toggleable Developer Mode**: Diagnostics panel offering traces of JNI wrappers, explicit logging levels, QoS Reliability parameters (Best-Effort vs. Reliable), and detailed diagnostic platform metrics.
7. **JNI Load Shielding**: Safe initializer wrapper that detects systems lacking native binaries and unlocks an automatic fallback simulator to guarantee stability, debugging, and usability across non-standard architectures.

---

## 🛠️ Phase 1 — How to Rebuild and Install

### 1. Host Machine Prerequisites
Validate machine dependencies using the pre-built checker script:
```bash
chmod +x ./setup_checker.sh
./setup_checker.sh
```

Ensure you have:
- **JDK 17** or above installed.
- **Android SDK** installed and `$ANDROID_HOME` configured.
- Modern Android Emulator or Physical device running **Android 11 (API Level 30)** or higher.

### 2. Build the Debug APK
Build the debug installment binary using Gradle:
```bash
gradle assembleDebug
```
*Tip: The build system compiles the Gradle codebase and places the finished, installable APK in the standard output path:*
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Sideload and Install
To install the APK directly to your connected USB Android device or local emulator run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 Swarm Integration & Next-Step Suggestions

Once the APK is running on your Android units, you can scale it into a fully orchestrated swarm deployment.

### 1. Connecting your Android Node to the Swarm
- **Local LAN Router**: Run a standard Zenoh Router daemon (`zenohd`) on a central host compute unit (or ground station):
  ```bash
  zenohd --listen tcp/0.0.0.0:7447
  ```
  On the Android app under *Zenoh Target Endpoint*, enter `tcp/<host-ip-address>:7447` and tap **Start System Bus Connection**.
- **Local Emulation**: If testing using the Android Studio Emulator, the host machine is located at `10.0.2.2`. Setup the endpoint in the app to: `tcp/10.0.2.2:7447`.

### 2. Topic Architecture Conventions for Swarms
Utilize structured path parameters (Key Expressions) to maximize bandwidth efficiency:
- `swarm/bus/uav_<id>/telemetry` - Sensor packages, battery status, coordinate grids.
- `swarm/bus/uav_<id>/heartbeat` - Live presence checks.
- `swarm/bus/command` - Distributed execution guidelines, urgent target assignments.

### 3. Letta Agent Bridge Setup
To bridge Zenoh bus telemetry feeds to **Letta** (cognitive memory agent framework):
1. Spawn a lightweight Python Python-Zenoh listener on the Letta host server.
2. Listen in on the wildcard topic `swarm/bus/**`:
   ```python
   import zenoh
   
   session = zenoh.open()
   sub = session.declare_subscriber('swarm/bus/**', lambda sample: forward_to_letta_api(sample))
   ```
3. Inside `forward_to_letta_api()`, POST the payload JSON directly to Letta's agent memory ingestion endpoint:
   `POST /agents/{agent_id}/messages`

---

## 📁 File Structure

```
├── app/
│   ├── build.gradle.kts           # App Gradle build descriptor
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # Permissions, JNI configurations
│           └── java/com/example/
│               ├── MainActivity.kt # Premium Dashboard (Compose UI)
│               └── ZenohViewModel.kt# Core background thread executor
├── metadata.json                 # Project Platform Identity
├── setup_checker.sh              # Host Environment Validator script
├── RESEARCH.md                   # Core Zenoh protocol study summary
└── README.md                     # Onboarding Developer Guide
```
