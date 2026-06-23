# Eclipse Zenoh Pub/Sub Research Summary

This document outlines the system architecture, core concepts, and Kotlin/Android integration patterns for **Eclipse Zenoh** (`zenoh-kotlin-android`), based on Zenoh protocol specifications.

---

## 1. Core Architectural Concepts

Eclipse Zenoh is a zero-overhead, ultra-low latency, and high-throughput protocol designed for edge computing, IoT, and swarm systems. It unifies data-in-motion (pub/sub), data-at-rest (query/storage), and computations.

### Pub/Sub (Publish/Subscribe)
Traditional protocols separate pub/sub (e.g., MQTT) from query/storage (e.g., HTTP/SQL). Zenoh unifies both. High-frequency telemetry can be pub/sub, with historical values queryable from storage via the exact same Key Expressions.

### Queries and Storage
- **Queryables**: Services that register to respond to on-demand `Get` queries instead of passively subscribing. 
- **Storages**: Built-in or custom databases that subscribe to paths, cache values, and automatically answer queries targeted at those paths.

### Liveliness
Liveliness allows peers in high-churn swarm networks to detect the presence, joining, or leaving of other members. It operates via tokens over multicast or peer sessions, avoiding heavy TCP keep-alive pings.

### Peer-to-Peer vs Client-Router Modes
Zenoh can operate in two primary topology configurations:
1. **Client-Router Mode**: Low-resource clients connect to a `Zenoh Router` (`zenohd`). The router handles routing tables, message brokering, and peer-to-peer delegation. Recommended for mobile nodes with NATs or firewalls.
2. **Peer-to-Peer Mode**: Nodes communicate directly with each other without a central broker. This is perfect for local Wi-Fi mesh networks or multi-car swarm swarming, but requires reachable IP address spaces (no strict NATs block).

### Key Expressions (KeyExpr)
Instead of arbitrary text strings, Zenoh uses highly optimized hierarchical paths called **Key Expressions** (e.g., `swarm/bus/uav_01/heartbeat`). Key expressions support wildcards:
- `*` matches a single path segment.
- `**` matches recursive path segments (e.g., `swarm/bus/**` matches anything starting with `swarm/bus/`).

### Low-Overhead Design & Fragmentation/Batching
- Zenoh boasts a minimal wire overhead (down to **5 bytes** per packet) compared to MQTT or DDS.
- Zenoh natively handles **fragmentation** of large payloads (e.g. video feeds) and **batching** of tiny payloads into single frames to maximize packet-per-second throughput and reduce overhead over constrained links.

---

## 2. Android & Kotlin Support

Eclipse Zenoh provides official Android bindings via `org.eclipse.zenoh:zenoh-kotlin-android`.

### How It Works (Rust JNI Core)
Under the hood, Zenoh is written in high-performance Rust for safety and zero-overhead. Native bindings (`zenoh-kotlin-android`) embed the Rust core via JNI (Java Native Interface). The Rust code is precompiled for all major Android ABIs:
- **`arm64-v8a`** / **`armeabi-v7a`** (Physical phones/drones)
- **`x86_64`** / **`x86`** (Android Studio Emulators)

### Requirements and Configuration
- **Minimum SDK**: **30** (Android 11) is required due to modern POSIX socket behaviors and filesystem JNI calls.
- **Required Permissions**:
  - `android.permission.INTERNET` (Network communication)
  - `android.permission.ACCESS_NETWORK_STATE` (Network health monitoring)

---

## 3. Official Demo and Reference Architectures

The official demo app found in `eclipse-zenoh/zenoh-demos/zenoh-android` provides a baseline Android application showing:
1. **Zenoh Session Control**: Connecting to `tcp/<ip>:<port>` or UDP multicast.
2. **Publisher View**: Setting a KeyExpr and publishing text strings or throughput metrics.
3. **Subscriber View**: Registering callbacks on key expressions and printing received messages.
4. **Query View**: Initiating on-demand `Get` query actions.

---

## 4. Key Kotlin Integration Code Patterns

### Establish Connection Session
```kotlin
import org.eclipse.zenoh.Config
import org.eclipse.zenoh.Session

val config = Config.defaultConfig().apply {
    // Connect to a Zenoh Router or PEER
    insertValue("connect/endpoints", "tcp/192.168.1.100:7447")
    insertValue("mode", "client") // "client" or "peer"
}

// Open the session synchronously or inside a coroutine
val session = Session.open(config)
```

### Publish Telemetry/Command Messages
```kotlin
import org.eclipse.zenoh.keyexpr.KeyExpr
import org.eclipse.zenoh.value.Value

val topic = "swarm/bus/heartbeat"
val keyExpr = KeyExpr.tryFrom(topic).getOrThrow()

// Simple publish or cached publisher
session.put(keyExpr, Value("{\"status\": \"active\", \"battery\": 88}"))
```

### Subscribing to Topic Streams
```kotlin
import org.eclipse.zenoh.keyexpr.KeyExpr
import org.eclipse.zenoh.Sample

val subTopic = "swarm/bus/**"
val keyExpr = KeyExpr.tryFrom(subTopic).getOrThrow()

val subscriber = session.declareSubscriber(
    keyExpr,
    { sample: Sample ->
        val receivedTopic = sample.keyExpr.toString()
        val payload = sample.value.toString()
        println("Received payload on $receivedTopic: $payload")
    }
)

// To stop listening and clean up JNI resources:
subscriber.close()
```

### Querying historical state or RPCs
```kotlin
import org.eclipse.zenoh.keyexpr.KeyExpr

val queryExpr = KeyExpr.tryFrom("swarm/bus/device_status").getOrThrow()
session.get(queryExpr) { reply ->
    val key = reply.sample?.keyExpr.toString()
    val value = reply.sample?.value?.toString() ?: "Empty"
    println("Query Result: $key -> $value")
}
```
