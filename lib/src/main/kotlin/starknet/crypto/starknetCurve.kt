@file:JvmName("StarknetCurve")

package starknet.crypto

import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.provider.JCEECPrivateKey
import org.bouncycastle.jce.provider.JCEECPublicKey
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import types.Felt
import java.math.BigInteger
import java.security.KeyPair
import java.security.Signature


private val provider = BouncyCastleProvider()

fun getKeyPair(privateKey: BigInteger): KeyPair {
    val q = DOMAIN.getG().multiply(privateKey);
    val publicParams = ECPublicKeyParameters(q, DOMAIN)
    return KeyPair(
        JCEECPublicKey("starknet-curve", publicParams, SPEC),
        JCEECPrivateKey("starknet-curve", ECPrivateKeySpec(privateKey, SPEC)),
    )
}

fun getStarkKey(keyPair: KeyPair): String {
    val key = (keyPair.public as JCEECPublicKey)
    return "0x" + BigInteger(1, key.q.xCoord.encoded).toString(16);
}

fun sign(keyPair: KeyPair, msg: ByteArray): StarknetCurveSignature {
    val ecdsaSign = Signature.getInstance("SHA256withECDSA", provider).apply {
        initSign(keyPair.private)
        update(msg)
    }

    return StarknetCurveSignature.fromASN1(ecdsaSign.sign())
}

fun verify(keyPair: KeyPair, msgHash: ByteArray, signature: StarknetCurveSignature): Boolean {
    val ecdsaVerify = Signature.getInstance("SHA256withECDSA", provider).apply {
        initVerify(keyPair.public)
        update(msgHash)
    }

    return ecdsaVerify.verify(signature.toASN1())
}