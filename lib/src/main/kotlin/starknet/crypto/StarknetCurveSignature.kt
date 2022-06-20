package starknet.crypto

import org.bouncycastle.asn1.*
import types.Felt
import types.toFelt
import java.io.ByteArrayOutputStream
import java.math.BigInteger

data class StarknetCurveSignature(val r: Felt, val s: Felt) {
    companion object {
        @JvmStatic
        fun fromASN1(input: ByteArray): StarknetCurveSignature {
            val ret = ASN1Sequence.getInstance(input)
            val r = (ret.getObjectAt(0) as ASN1Integer).positiveValue
            val s = (ret.getObjectAt(1) as ASN1Integer).positiveValue
            return StarknetCurveSignature(r.toFelt, s.toFelt)
        }
    }

    fun toASN1(): ByteArray {
        val vector = ASN1EncodableVector().also {
            it.add(ASN1Integer(r.value))
            it.add(ASN1Integer(s.value))
        }
        val resultStream = ByteArrayOutputStream()
        val stream = ASN1OutputStream.create(resultStream)
        stream.writeObject(DERSequence(vector))
        stream.flush()

        return resultStream.toByteArray()
    }
}