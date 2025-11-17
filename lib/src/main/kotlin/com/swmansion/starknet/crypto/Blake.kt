/**
 * This module's Blake2s felt encoding and hashing logic is based on StarkWare's
 * sequencer implementation:
 * https://github.com/starkware-libs/sequencer/blob/b29c0e8c61f7b2340209e256cf87dfe9f2c811aa/crates/blake2s/src/lib.rs
 */
package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toFelt
import org.bouncycastle.crypto.digests.Blake2sDigest
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN

object Blake {

    private val smallThreshold: BigInteger = BigInteger.ONE.shiftLeft(63)
    private const val BIG_MARKER = 1 shl 31

    /**
     * Encodes a list of felts into 32-bit words following Cairo's encoding scheme.
     *
     * @param values List of Felt values to encode.
     * @return List of encoded 32-bit integers.
     */
    private fun encodeFeltsToInts(values: List<Felt>): List<Int> {
        val unpackedInts = mutableListOf<Int>()

        for (value in values) {
            val valueBytes = value.value.toByteArray().let {
                if (it.size < 32) {
                    ByteArray(32 - it.size) + it
                } else {
                    it.copyOfRange(it.size - 32, it.size)
                }
            }

            if (value.value < smallThreshold) {
                // Small: 2 limbs only (high-32 then low-32 of the last 8 bytes)
                val high = BigInteger(1, valueBytes.copyOfRange(24, 28)).toInt()
                val low = BigInteger(1, valueBytes.copyOfRange(28, 32)).toInt()
                unpackedInts.add(high)
                unpackedInts.add(low)
            } else {
                // Big: 8 limbs, big-endian order
                val start = unpackedInts.size
                for (i in 0 until 32 step 4) {
                    val limb = BigInteger(1, valueBytes.copyOfRange(i, i + 4)).toInt()
                    unpackedInts.add(limb)
                }
                unpackedInts[start] = unpackedInts[start] or BIG_MARKER
            }
        }
        return unpackedInts
    }

    /**
     * Packs the first 32 bytes of a byte array (little-endian) into a Felt.
     *
     * @param hashBytes Byte array containing the hash.
     * @return Felt representation of the packed value.
     */
    private fun pack256LeToFelt(hashBytes: ByteArray): Felt {
        require(hashBytes.size >= 32) { "need at least 32 bytes to pack" }

        val reversed = hashBytes.copyOfRange(0, 32).reversedArray()
        val value = BigInteger(1, reversed)
        return value.mod(Felt.PRIME).toFelt
    }

    private fun blake2sHashBytes(vararg inputs: ByteArray): ByteArray {
        val digest = Blake2sDigest(256)
        for (input in inputs) {
            digest.update(input, 0, input.size)
        }
        val hash = ByteArray(32)
        digest.doFinal(hash, 0)
        return hash
    }

    /**
     * Computes the Blake2s hash of the given data and converts it to a Felt.
     *
     * @param data Byte array to hash.
     * @return Felt representation of the hash.
     */
    private fun blake2sToFelt(data: ByteArray): Felt =
        pack256LeToFelt(blake2sHashBytes(data))

    /**
     * Computes the Blake2s hash of a list of Felts.
     *
     * @param felts List of Felt values to hash.
     * @return Felt representation of the hash.
     */
    @JvmStatic
    fun blake2sHash(felts: List<Felt>): Felt {
        val words = encodeFeltsToInts(felts)

        // Serialize as little-endian bytes
        val byteArray = words.flatMap { word ->
            ByteBuffer.allocate(4).order(LITTLE_ENDIAN).putInt(word).array().asList()
        }.toByteArray()

        return blake2sToFelt(byteArray)
    }
}
