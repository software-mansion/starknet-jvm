package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.BlockWithTransactionsResponse
import com.swmansion.starknet.data.types.GetBlockWithTransactionsResponse
import com.swmansion.starknet.data.types.PendingBlockWithTransactionsResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object JsonRpcGetBlockWithTransactionsPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithTransactionsResponse>(GetBlockWithTransactionsResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out GetBlockWithTransactionsResponse> {
        val isPendingBlock = listOf("block_hash", "block_number", "new_root").any { it !in element.jsonObject }

        return when (isPendingBlock) {
            true -> PendingBlockWithTransactionsResponse.serializer()
            false -> BlockWithTransactionsResponse.serializer()
        }
    }
}
