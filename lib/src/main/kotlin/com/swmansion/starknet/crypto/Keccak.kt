@file:JvmName("Keccak")

package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toFelt
import org.bouncycastle.jcajce.provider.digest.Keccak
import java.math.BigInteger

private val MASK_250 = BigInteger.valueOf(2).pow(250) - BigInteger.ONE

/**
 * Compute a keccak hash.
 *
 * Computes a keccak of provided input and applies 250bit mask to fit it in a felt.
 *
 * @param input a ByteArray from which keccak will be generated
 * @return a felt with computed keccak
 */
fun starknetKeccak(input: ByteArray): Felt = keccak(input).and(MASK_250).toFelt

/**
 * Compute a keccak hash.
 *
 * Computes a keccak of provided input.
 *
 * @param input a ByteArray from which keccak will be generated
 * @return a big integer with computed keccak
 */
fun keccak(input: ByteArray): BigInteger {
    val keccak = Keccak.Digest256().apply {
        update(input)
    }
    return BigInteger(keccak.digest())
}
