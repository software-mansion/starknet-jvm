package com.swmansion.starknet.data.serializers

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

internal object GatewayGetBlockTransactionCountSerializer :
    JsonTransformingSerializer<Int>(Int.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) return JsonPrimitive(0)
        return JsonPrimitive(element.size)
    }
}
