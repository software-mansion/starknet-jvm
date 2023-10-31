package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt

@JvmSynthetic
fun Array<Felt>.toCalldata(): List<Felt> {
    return this.toList()
}

@JvmSynthetic
fun Collection<Felt>.toCalldata(): List<Felt> {
    return this.toList()
}
