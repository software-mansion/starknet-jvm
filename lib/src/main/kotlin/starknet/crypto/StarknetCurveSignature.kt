package starknet.crypto

import starknet.data.types.Felt

/**
 * Class representing a starknet curve signature
 *
 * @param r part of the signature
 * @param s part of the signature
 */
data class StarknetCurveSignature(val r: Felt, val s: Felt) {
    fun toList(): List<Felt> = listOf(r, s)
}
