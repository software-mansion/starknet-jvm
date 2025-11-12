package com.swmansion.starknet.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.extensions.toFelt
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Security

object Blake {

    private val smallThreshold: BigInteger = BigInteger.ONE.shiftLeft(63)
    private val bigMarker = 1 shl 31

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * Encodes a list of felts into 32-bit words following Cairo's encoding scheme.
     *
     * @param values List of Felt values to encode.
     * @return List of encoded 32-bit integers.
     */
    private fun encodeFeltsToU32s(values: List<Felt>): List<Int> {
        val unpackedU32s = mutableListOf<Int>()

        for (value in values) {
            val valueBytes = value.value.toByteArray().let {
                // ensure 32-byte big-endian representation
                when {
                    it.size < 32 -> ByteArray(32 - it.size) + it
                    it.size > 32 -> it.copyOfRange(it.size - 32, it.size)
                    else -> it
                }
            }

            if (value.value < smallThreshold) {
                // Small: 2 limbs only (high-32 then low-32 of the last 8 bytes)
                val high = BigInteger(1, valueBytes.copyOfRange(24, 28)).toInt()
                val low = BigInteger(1, valueBytes.copyOfRange(28, 32)).toInt()
                unpackedU32s.add(high)
                unpackedU32s.add(low)
            } else {
                // Big: 8 limbs, big-endian order
                val start = unpackedU32s.size
                for (i in 0 until 32 step 4) {
                    val limb = BigInteger(1, valueBytes.copyOfRange(i, i + 4)).toInt()
                    unpackedU32s.add(limb)
                }
                unpackedU32s[start] = unpackedU32s[start] or bigMarker
            }
        }
        return unpackedU32s
    }

    /**
     * Packs the first 32 bytes of a byte array (little-endian) into a Felt.
     *
     * @param hashBytes Byte array containing the hash.
     * @return Felt representation of the packed value.
     */
    private fun pack256LeToFelt(hashBytes: ByteArray): Felt {
        require(hashBytes.size >= 32) { "need at least 32 bytes to pack" }

        var value = BigInteger.ZERO
        for (i in 31 downTo 0) {
            value = value.shiftLeft(8).add(BigInteger.valueOf((hashBytes[i].toInt() and 0xFF).toLong()))
        }
        return value.mod(Felt.PRIME).toFelt
    }

    /**
     * Computes the Blake2s hash of the given data and converts it to a Felt.
     *
     * @param data Byte array to hash.
     * @return Felt representation of the hash.
     */
    private fun blake2sToFelt(data: ByteArray): Felt {
        val digest = MessageDigest.getInstance("BLAKE2S-256", "BC")
        val hash = digest.digest(data)
        return pack256LeToFelt(hash)
    }

    /**
     * Computes the Blake2s hash of a list of Felts.
     *
     * @param felts List of Felt values to hash.
     * @return Felt representation of the hash.
     */
    @JvmStatic
    fun blake2sHash(felts: List<Felt>): Felt {
        val u32Words = encodeFeltsToU32s(felts)

        // Serialize as little-endian bytes
        val byteStream = u32Words.flatMap { word ->
            val bytes = ByteArray(4)
            for (i in 0 until 4) {
                bytes[i] = ((word shr (8 * i)) and 0xFF).toByte()
            }
            bytes.asList()
        }.toByteArray()

        return blake2sToFelt(byteStream)
    }

    /**
     * Converts a Felt to its native byte array representation (32 bytes, big-endian).
     *
     * @param felt Felt value to convert.
     * @return Byte array representation of the Felt.
     */
    @JvmStatic
    fun blake2sHash(first: ByteArray, second: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("BLAKE2S-256", "BC")
        digest.update(first)
        return digest.digest(second)
    }

    /**
     * Converts pair of Felts to their native byte array representations and computes their Blake2s hash.
     *
     * @param first Felt value to convert.
     * @param second Felt value to convert.
     * @return Byte array representation of the hash.
     */
    @JvmStatic
    fun blake2sHash(first: Felt, second: Felt): Felt {
        val hash = blake2sHash(feltToNative(first), feltToNative(second))
        return pack256LeToFelt(hash)
    }

    /**
     * Converts an iterable of Felts to their native byte array representations and computes their Blake2s hash.
     *
     * @param values Iterable of Felt values to convert.
     * @return Felt representation of the hash.
     */
    @JvmStatic
    fun blake2sHash(values: Iterable<Felt>): Felt =
        values.fold(Felt.ZERO) { a, b -> blake2sHash(a, b) }

}