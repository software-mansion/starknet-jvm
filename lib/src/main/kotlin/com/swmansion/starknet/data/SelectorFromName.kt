@file:JvmName("Selector")

package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.keccak
import com.swmansion.starknet.data.types.Felt

fun selectorFromName(name: String): Felt {
    if (name == DEFAULT_ENTRY_POINT_NAME || name == DEFAULT_L1_ENTRY_POINT_NAME) {
        return Felt(DEFAULT_ENTRY_POINT_SELECTOR)
    }
    return keccak(name.toByteArray(Charsets.US_ASCII))
}
