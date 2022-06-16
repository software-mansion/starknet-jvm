package crypto

import crypto.StarknetCurve.SAMPLED_GENERATORS
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.JCEECPrivateKey
import org.bouncycastle.jce.provider.JCEECPublicKey
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import types.Felt
import java.math.BigInteger
import java.security.KeyPair
import java.security.Signature


object StarknetCrypto {
    private val provider = BouncyCastleProvider()

    fun getKeyPair(privateKey: BigInteger): KeyPair {
        val q = StarknetCurve.DOMAIN.getG().multiply(privateKey);
        val publicParams = ECPublicKeyParameters(q, StarknetCurve.DOMAIN)
        val spec = StarknetCurve.SPEC
        return KeyPair(
            JCEECPublicKey("starknet-curve", publicParams, spec),
            JCEECPrivateKey("starknet-curve", ECPrivateKeySpec(privateKey, spec)),
        )
    }

    fun getStarkKey(keyPair: KeyPair): String {
        val key = (keyPair.public as JCEECPublicKey)
        return "0x" + BigInteger(1, key.q.xCoord.encoded).toString(16);
    }

    fun sign(keyPair: KeyPair, msg: ByteArray): ECSignature {
        val ecdsaSign = Signature.getInstance("SHA256withECDSA", provider).also {
            it.initSign(keyPair.private)
            it.update(msg)
        }

        return ECSignature.fromASN1(ecdsaSign.sign())
    }

    fun verify(keyPair: KeyPair, msgHash: ByteArray, signature: ECSignature): Boolean {
        val ecdsaVerify = Signature.getInstance("SHA256withECDSA", provider).also {
            it.initVerify(keyPair.public)
            it.update(msgHash)
        }

        return ecdsaVerify.verify(signature.toASN1())
    }

    fun pedersen(first: Felt, second: Felt): Felt {
        var point = SAMPLED_GENERATORS[0]
        for (i in 0..1) {
            var x = if (i == 0) first.value else second.value
            for (j in 0 until 252) {
                val iterationPoint = SAMPLED_GENERATORS[2 + i * 252 + j]
                assert(point.xCoord != iterationPoint.xCoord)

                if (x.testBit(0)) {
                    point = point.add(iterationPoint);
                }
                x = x.shiftRight(1)
            }
        }

        return Felt(point.normalize().xCoord.toBigInteger())
    }

    fun pedersen(values: Iterable<Felt>): Felt {
        return values.fold(Felt.ZERO) { a, b -> pedersen(a, b) }
    }
}