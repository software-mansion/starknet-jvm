package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject

object GatewayCallContractTransformingSerializer :
    JsonTransformingSerializer<List<Felt>>(ListSerializer(Felt.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return element.jsonObject["result"]!!
    }
}
