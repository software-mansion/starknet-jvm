@file:JvmName("Selector")

package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.starknetKeccak
import com.swmansion.starknet.data.types.Felt

const val DEFAULT_ENTRY_POINT_NAME = "__default__"
const val DEFAULT_L1_ENTRY_POINT_NAME = "__l1_default__"
const val DEFAULT_ENTRY_POINT_SELECTOR = 0
const val EXECUTE_ENTRY_POINT_NAME = "__execute__"

/**
 * Get a Felt value of contract's entry point selector provided as a string
 *
 * @param name a name of the entrypoint
 * @return Felt value of the entrypoint selector
 */
fun selectorFromName(name: String): Felt {
    if (name == DEFAULT_ENTRY_POINT_NAME || name == DEFAULT_L1_ENTRY_POINT_NAME) {
        return Felt(DEFAULT_ENTRY_POINT_SELECTOR)
    }
    return starknetKeccak(name.toByteArray(Charsets.US_ASCII))
}
