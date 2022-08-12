package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

internal object GatewayTransactionTransformingSerializer : JsonTransformingSerializer<Transaction>(Transaction.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject["transaction"]!!
    }
}
