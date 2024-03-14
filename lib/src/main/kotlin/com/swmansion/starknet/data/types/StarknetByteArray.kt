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
    /**
     * Encode as a Felt list
     */
    override fun toCalldata(): List<Felt> {
        return listOf(data.size.toFelt) + data + listOf(pendingWord, pendingWordLen.toFelt)
    }

    companion object {
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
