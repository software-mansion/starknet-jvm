package com.swmansion.starknet.data.responses.serializers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import com.swmansion.starknet.data.responses.Transaction

object GatewayTransactionTransformingSerializer : JsonTransformingSerializer<Transaction>(Transaction.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject["transaction"]!!
    }
}
