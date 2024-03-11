package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.splitToShortStrings
import com.swmansion.starknet.extensions.toFelt

data class StarknetByteArray(
    val data: List<Felt>,
    val pendingWord: Felt,
    val pendingWordLen: Int,
) : ConvertibleToCalldata {
    override fun toCalldata(): List<Felt> {
        return listOf(data.size.toFelt) + data + listOf(pendingWord, pendingWordLen.toFelt)
    }

    companion object {
        fun fromString(string: String): StarknetByteArray {
            val shortStrings = string.splitToShortStrings()
            val encodedShortStrings = shortStrings.map(Felt::fromShortString)

            val (data, pendingWord, pendingWordLen) = if (shortStrings.isEmpty() || shortStrings.last().length == 31)
                Triple(encodedShortStrings, Felt.ZERO, 0)
            else
                Triple(encodedShortStrings.dropLast(1), encodedShortStrings.last(), shortStrings.last().length)

            return StarknetByteArray(data.ifEmpty { listOf(Felt.ZERO) }, pendingWord, pendingWordLen)
        }
    }
}
