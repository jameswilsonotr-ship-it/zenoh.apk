package org.eclipse.zenoh.value

class Value(private val payload: String) {
    override fun toString(): String {
        return payload
    }
}
