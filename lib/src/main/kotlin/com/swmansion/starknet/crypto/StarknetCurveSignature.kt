package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata

/**
 * Class representing a starknet curve signature
 *
 * @param r part of the signature
 * @param s part of the signature
 */
data class StarknetCurveSignature(val r: Felt, val s: Felt) : ConvertibleToCalldata {
    fun toList(): List<Felt> = listOf(r, s)

    override fun toCalldata(): List<Felt> {
        return toList()
    }
}
