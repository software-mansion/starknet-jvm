package com.swmansion.starknet.extensions

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata

fun List<ConvertibleToCalldata>.toCalldata(): List<Felt> {
    return this.flatMap { it.toCalldata() }
}
