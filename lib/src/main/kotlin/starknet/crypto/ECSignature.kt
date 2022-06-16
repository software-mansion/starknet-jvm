package crypto

import org.bouncycastle.asn1.*
import java.io.ByteArrayOutputStream
import java.math.BigInteger

data class ECSignature(val r: BigInteger, val s: BigInteger) {
    companion object {
        fun fromASN1(input: ByteArray): ECSignature {
            val ret = ASN1Sequence.getInstance(input)
            val r = (ret.getObjectAt(0) as ASN1Integer).positiveValue
            val s = (ret.getObjectAt(1) as ASN1Integer).positiveValue
            return ECSignature(r, s)
        }
    }

    fun toASN1(): ByteArray {

        val vector = ASN1EncodableVector().also {
            it.add(ASN1Integer(r))
            it.add(ASN1Integer(s))
        }
        val resultStream = ByteArrayOutputStream()
        val stream = ASN1OutputStream.create(resultStream)
        stream.writeObject(DERSequence(vector))
        stream.flush()

        return resultStream.toByteArray()
    }
}