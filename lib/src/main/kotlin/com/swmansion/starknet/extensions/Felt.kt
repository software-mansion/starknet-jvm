package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import java.math.BigInteger

@JvmSynthetic
internal fun List<Felt>.toDecimal(): List<String> {
    return this.map {
        it.decString()
    }
}

@get:JvmSynthetic
internal val BigInteger.toFelt: Felt
    get() = Felt(this)
