package com.swmansion.starknet.data.types.conversions

import com.swmansion.starknet.data.types.Felt

/**
 * Implementers of this interface support conversions to
 * the list of Felts.
 */
interface ConvertibleToCalldata {
    /**
     * @return this object as a list of Felts
     */
    fun toCalldata(): List<Felt>
}
