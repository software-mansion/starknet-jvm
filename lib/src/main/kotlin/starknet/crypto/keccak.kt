package starknet.crypto

import org.bouncycastle.jcajce.provider.digest.Keccak
import types.Felt
import types.toFelt
import java.math.BigInteger

private val MASK_250 = BigInteger.TWO.pow(250) - BigInteger.ONE

fun keccak(input: ByteArray): Felt {
    val keccak = Keccak.Digest256().apply {
        update(input)
    }
    return BigInteger(keccak.digest()).and(MASK_250).toFelt
}