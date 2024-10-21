package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.provider.rpc.*
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
        val data = jsonObject["data"]?.let {
            when (it) {
                is JsonPrimitive -> it.jsonPrimitive.content
                is JsonArray -> it.jsonArray.toString()
                is JsonObject -> it.jsonObject.toString()
            }
        }

        return when (code) {
            1 -> FailedToReceiveTransactionError()
            20 -> ContractNotFoundError()
            24 -> BlockNotFoundError()
            27 -> InvalidTransactionIndexError()
            28 -> ClassHashNotFoundError()
            29 -> TransactionHashNotFoundError()
            31 -> PageSizeTooBigError()
            32 -> NoBlocksError()
            33 -> InvalidContinuationTokenError()
            34 -> TooManyKeysInFilterError()
            40 -> ContractError(data = data!!)
            41 -> TransactionExecutionError(data = data!!)
            else -> throw IllegalArgumentException("Unknown JSON RPC error code: $code")
        }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("JsonRpcError", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonRpcError) {
        throw SerializationException("Class used for deserialization only.")
    }
}
