package com.swmansion.starknet.data.serializers

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

internal object GatewayGetBlockNumberSerializer :
    JsonTransformingSerializer<Int>(Int.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject["block_number"]!!
    }
}
