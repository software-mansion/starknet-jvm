@file:JvmName("Felt")
package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import java.math.BigInteger

fun List<Felt>.toDecimal(): List<String> {
    return this.map {
        it.decString()
    }
}

val BigInteger.toFelt: Felt
    get() = Felt(this)
