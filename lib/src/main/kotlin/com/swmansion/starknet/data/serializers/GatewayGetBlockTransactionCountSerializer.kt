package com.swmansion.starknet.data.serializers

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

internal object GatewayGetBlockTransactionCountSerializer :
    JsonTransformingSerializer<Int>(Int.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val transactions = element.jsonObject["transactions"]
        if (transactions !is JsonArray) return JsonPrimitive(0)
        return JsonPrimitive(transactions.size)
    }
}
