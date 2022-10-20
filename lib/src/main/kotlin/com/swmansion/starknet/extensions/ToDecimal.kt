package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt

@JvmSynthetic
internal fun List<Felt>.toDecimal(): List<String> {
    return this.map {
        it.decString()
    }
}
