@file:JvmName("Felt")

package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import java.math.BigInteger

internal fun List<Felt>.toDecimal(): List<String> {
    return this.map {
        it.decString()
    }
}

internal val BigInteger.toFelt: Felt
    get() = Felt(this)
