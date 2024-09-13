package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.splitToShortStrings
import com.swmansion.starknet.extensions.toFelt

/**
 * Represents a ByteArray struct from Cairo.
 *
 * The ByteArray struct is used to represent a string in Cairo.
 *
 * @param data The list of 31-byte chunks of the byte array
 * @param pendingWord The last chunk of the byte array, which consists of at most 30 bytes
 * @param pendingWordLen The number of bytes in [pendingWord]
 */
data class StarknetByteArray(
    val data: List<Felt>,
    val pendingWord: Felt,
    val pendingWordLen: Int,
) : ConvertibleToCalldata {
    init {
        data.forEach {
            require(it.byteLength == 31) { "All elements of 'data' must be 31 bytes long. [${it.hexString()}] of length [${it.byteLength}] given." }
        }
        require(pendingWordLen in 0..30) {
            "The length of 'pendingWord' must be between 0 and 30. [$pendingWordLen] given."
        }
        // We skip the edge case when pending word is a null character, because its pendingWord.byteLength is 0 and pendingWordLen is 1.
        if (!(pendingWord == Felt.ZERO && pendingWordLen == 1)) {
            require(pendingWord.byteLength == pendingWordLen) {
                "The length of 'pendingWord' must be equal to 'pendingWordLen'. [${pendingWord.hexString()}] of length [${pendingWord.byteLength}] given."
            }
        }
    }

    /**
     * Encode as a Felt list
     */
    override fun toCalldata(): List<Felt> {
        return listOf(data.size.toFelt) + data + listOf(pendingWord, pendingWordLen.toFelt)
    }

    /**
     * Encode as a String
     */
    override fun toString(): String {
        // Handle edge case when pending word is null character.
        val pendingWordShortString = if (pendingWord == Felt.ZERO && pendingWordLen == 0) "" else pendingWord.toShortString()
        val shortStrings = data.map { it.toShortString() } + pendingWordShortString
        return shortStrings.joinToString(separator = "")
    }

    companion object {
        /**
         * Create byte array from a string.
         *
         * @param string The string to be transformed to byte array
         */
        @JvmStatic
        fun fromString(string: String): StarknetByteArray {
            val shortStrings = string.splitToShortStrings()
            val encodedShortStrings = shortStrings.map(Felt::fromShortString)

            return if (shortStrings.isEmpty() || shortStrings.last().length == 31)
                StarknetByteArray(encodedShortStrings, Felt.ZERO, 0)
            else
                StarknetByteArray(encodedShortStrings.dropLast(1), encodedShortStrings.last(), shortStrings.last().length)
        }
    }
}

internal val Felt.byteLength: Int
    get() = (value.bitLength() + 7) / 8
