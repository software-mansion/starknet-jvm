package starknet.crypto

import org.bouncycastle.jcajce.provider.digest.Keccak
import starknet.data.types.Felt
import starknet.data.types.toFelt
import java.math.BigInteger

private val MASK_250 = BigInteger.valueOf(2).pow(250) - BigInteger.ONE

/**
 * Compute a keccak.
 *
 * Computes a keccak of provided input.
 *
 * @param input a ByteArray from which keccak will be generated
 * @return a felt with computed keccak
 */
fun keccak(input: ByteArray): Felt {
    val keccak = Keccak.Digest256().apply {
        update(input)
    }
    return BigInteger(keccak.digest()).and(MASK_250).toFelt
}
