package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.provider.rpc.JsonRpcContractError
import com.swmansion.starknet.provider.rpc.JsonRpcError
import com.swmansion.starknet.provider.rpc.JsonRpcStandardError
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object JsonRpcErrorPolymorphicSerializer : JsonContentPolymorphicSerializer<JsonRpcError>(JsonRpcError::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<JsonRpcError> {
        val jsonElement = element.jsonObject

        val isContractError = "data" in jsonElement &&
            jsonElement["data"]!! is JsonObject &&
            "revert_error" in jsonElement["data"]!!.jsonObject &&
            jsonElement["data"]!!.jsonObject.size == 1
        return when {
            isContractError -> JsonRpcContractError.serializer()
            else -> JsonRpcStandardError.serializer()
        }
    }
}

internal object JsonRpcStandardErrorSerializer : KSerializer<JsonRpcStandardError> {
    override fun deserialize(decoder: Decoder): JsonRpcStandardError {
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

        return JsonRpcStandardError(
            code = code,
            message = message,
            data = data,
        )
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("JsonRpcStandardError", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonRpcStandardError) {
        throw SerializationException("Class used for deserialization only.")
    }
}
