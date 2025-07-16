package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

internal object StateUpdatePolymorphicSerializer : JsonContentPolymorphicSerializer<StateUpdate>(StateUpdate::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<StateUpdate> {
        val jsonElement = element.jsonObject
        val isPreConfirmed = "block_hash" !in jsonElement
        return if (isPreConfirmed) PreConfirmedStateUpdateResponse.serializer() else StateUpdateResponse.serializer()
    }
}
