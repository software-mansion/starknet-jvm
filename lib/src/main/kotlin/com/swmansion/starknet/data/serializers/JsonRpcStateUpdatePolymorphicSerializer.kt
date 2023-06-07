package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object JsonRpcStateUpdatePolymorphicSerializer : JsonContentPolymorphicSerializer<StateUpdate>(StateUpdate::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out StateUpdate> {
        val jsonElement = element.jsonObject
        val isPending = "block_hash" !in jsonElement
        return if (isPending) PendingStateUpdateResponse.serializer() else StateUpdateResponse.serializer()
    }
}
