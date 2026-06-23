package org.eclipse.zenoh.keyexpr

class KeyExpr private constructor(private val expr: String) {
    companion object {
        fun tryFrom(expr: String): Result<KeyExpr> = Result.success(KeyExpr(expr))
    }
    
    override fun toString(): String {
        return expr
    }
}
