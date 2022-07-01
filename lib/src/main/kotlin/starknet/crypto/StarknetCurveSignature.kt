package starknet.crypto

import types.Felt

data class StarknetCurveSignature(val r: Felt, val s: Felt) {
    fun toList(): List<Felt> = listOf(r, s)
}