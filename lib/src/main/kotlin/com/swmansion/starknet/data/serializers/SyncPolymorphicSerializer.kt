package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.NotSyncingResponse
import com.swmansion.starknet.data.types.Syncing
import com.swmansion.starknet.data.types.SyncingResponse
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object SyncPolymorphicSerializer : JsonContentPolymorphicSerializer<Syncing>(Syncing::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Syncing> = when (element) {
        JsonPrimitive(false) -> NotSyncingResponseSerializer
        else -> SyncingResponse.serializer()
    }
}

internal object NotSyncingResponseSerializer : KSerializer<NotSyncingResponse> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("NotSyncingResponse")

    override fun deserialize(decoder: Decoder): NotSyncingResponse {
        require(decoder is JsonDecoder)
        val status = decoder.decodeJsonElement().jsonPrimitive.booleanOrNull
            ?: throw SerializationException("Invalid NotSyncingResponse value [${decoder.decodeJsonElement().jsonPrimitive}], boolean expected.")
        return NotSyncingResponse(status)
    }

    override fun serialize(encoder: Encoder, value: NotSyncingResponse) {
        require(encoder is JsonEncoder)
        encoder.encodeJsonElement(JsonPrimitive(value.status))
    }
}
