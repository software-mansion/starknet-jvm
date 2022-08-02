package com.swmansion.starknet.data.responses.serializers

import com.swmansion.starknet.data.responses.Transaction
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

object GatewayTransactionTransformingSerializer : JsonTransformingSerializer<Transaction>(Transaction.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject["transaction"]!!
    }
}
