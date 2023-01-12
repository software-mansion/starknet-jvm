package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.BlockWithTransactionsResponse
import com.swmansion.starknet.data.types.GetBlockWithTransactionsResponse
import com.swmansion.starknet.data.types.PendingBlockWithTransactionsResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object JsonRpcGetBlockWithTransactionsPolymorphicSerializer : JsonContentPolymorphicSerializer<GetBlockWithTransactionsResponse>(GetBlockWithTransactionsResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out GetBlockWithTransactionsResponse> =
        when (element.jsonObject["status"]) {
            null -> PendingBlockWithTransactionsResponse.serializer()
            else -> BlockWithTransactionsResponse.serializer()
        }
}
