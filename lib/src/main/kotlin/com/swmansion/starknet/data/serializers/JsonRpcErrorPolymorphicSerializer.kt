package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.provider.rpc.JsonRpcError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object JsonRpcErrorSerializer : KSerializer<JsonRpcError> {
    override fun deserialize(decoder: Decoder): JsonRpcError {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonInput for ${decoder::class}")

        val jsonObject = input.decodeJsonElement().jsonObject

        val code = jsonObject.getValue("code").jsonPrimitive.content.toInt()
        val message = jsonObject.getValue("message").jsonPrimitive.content
        val data = jsonObject["data"]?.let {
            when (it) {
                is JsonPrimitive -> it.jsonPrimitive.content
                is JsonArray -> it.jsonArray.toString()
                is JsonObject -> it.jsonObject.toString()
            }
        }

        return JsonRpcError(
            code = code,
            message = message,
            data = data,
        )
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("JsonRpcError", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonRpcError) {
        throw SerializationException("Class used for deserialization only.")
    }
}
