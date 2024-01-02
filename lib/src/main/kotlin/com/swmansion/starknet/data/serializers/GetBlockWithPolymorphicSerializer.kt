package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.BlockWithTransactionHashesResponse
import com.swmansion.starknet.data.types.BlockWithTransactionsResponse
import com.swmansion.starknet.data.types.GetBlockWithTransactionHashesResponse
import com.swmansion.starknet.data.types.GetBlockWithTransactionsResponse
import com.swmansion.starknet.data.types.PendingBlockWithTransactionHashesResponse
import com.swmansion.starknet.data.types.PendingBlockWithTransactionsResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object GetBlockWithTransactionsPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithTransactionsResponse>(GetBlockWithTransactionsResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GetBlockWithTransactionsResponse> {
        val isPendingBlock = listOf("block_hash", "block_number", "new_root").any { it !in element.jsonObject }

        return when (isPendingBlock) {
            true -> PendingBlockWithTransactionsResponse.serializer()
            false -> BlockWithTransactionsResponse.serializer()
        }
    }
}

internal object GetBlockWithTransactionHashesPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithTransactionHashesResponse>(GetBlockWithTransactionHashesResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GetBlockWithTransactionHashesResponse> {
        val isPendingBlock = listOf("block_hash", "block_number", "new_root").any { it !in element.jsonObject }

        return when (isPendingBlock) {
            true -> PendingBlockWithTransactionHashesResponse.serializer()
            false -> BlockWithTransactionHashesResponse.serializer()
        }
    }
}
