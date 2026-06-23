package org.eclipse.zenoh

import org.eclipse.zenoh.keyexpr.KeyExpr
import org.eclipse.zenoh.value.Value

class Config {
    companion object {
        fun defaultConfig() = Config()
    }
    fun insertValue(key: String, value: String) {}
}

class Session private constructor() : AutoCloseable {
    companion object {
        fun open(config: Config) = Session()
    }
    
    fun declareSubscriber(keyExpr: KeyExpr, callback: (Sample) -> Unit): AutoCloseable {
        return AutoCloseable { /* No-op closing */ }
    }
    
    fun put(keyExpr: KeyExpr, value: Value) {}
    
    override fun close() {}
}

class Sample(val keyExpr: KeyExpr, val value: Value)
