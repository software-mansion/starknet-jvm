package starknet.crypto

import org.bouncycastle.asn1.*
import types.Felt
import types.toFelt
import java.io.ByteArrayOutputStream

data class StarknetCurveSignature(val r: Felt, val s: Felt) {
    fun toList(): List<Felt> = listOf(r, s)
}