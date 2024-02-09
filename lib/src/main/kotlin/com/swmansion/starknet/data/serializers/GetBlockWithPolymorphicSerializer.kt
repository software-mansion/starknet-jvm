package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal object GetBlockWithTransactionsPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithTransactionsResponse>(GetBlockWithTransactionsResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GetBlockWithTransactionsResponse> {
        return when (isPendingBlock(element.jsonObject)) {
            true -> PendingBlockWithTransactionsResponse.serializer()
            false -> BlockWithTransactionsResponse.serializer()
        }
    }
}

internal object GetBlockWithTransactionHashesPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithTransactionHashesResponse>(GetBlockWithTransactionHashesResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GetBlockWithTransactionHashesResponse> {
        return when (isPendingBlock(element.jsonObject)) {
            true -> PendingBlockWithTransactionHashesResponse.serializer()
            false -> BlockWithTransactionHashesResponse.serializer()
        }
    }
}

internal object GetBlockWithReceiptsPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithReceiptsResponse>(GetBlockWithReceiptsResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<GetBlockWithReceiptsResponse> {
        return when (isPendingBlock(element.jsonObject)) {
            true -> PendingBlockWithReceiptsResponse.serializer()
            false -> BlockWithReceiptsResponse.serializer()
        }
    }
}

private fun isPendingBlock(element: JsonObject): Boolean {
    return listOf("block_hash", "block_number", "new_root").any { it !in element }
}
