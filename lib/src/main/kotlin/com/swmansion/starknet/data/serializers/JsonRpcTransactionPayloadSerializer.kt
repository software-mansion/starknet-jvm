package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = TransactionPayload::class)
object JsonRpcTransactionPayloadSerializer : KSerializer<TransactionPayload> {

    override val descriptor: SerialDescriptor
        get() = TransactionPayload.serializer().descriptor

    // TODO: Consider removing deserialize methods altogether
    override fun deserialize(decoder: Decoder): TransactionPayload {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()

        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "INVOKE" -> decoder.json.decodeFromJsonElement(InvokeTransactionPayload.serializer(), element)
            "DECLARE" -> deserializeDeclare(decoder, element)
            "DEPLOY_ACCOUNT" -> decoder.json.decodeFromJsonElement(DeployAccountTransactionPayload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid transaction type '${element.jsonObject["type"]?.jsonPrimitive?.content}'")
        }
    }

    private fun deserializeDeclare(decoder: JsonDecoder, element: JsonElement): DeclareTransactionPayload =
        when (element.jsonObject["version"]?.jsonPrimitive?.content) {
            Felt.ONE.hexString() -> decoder.json.decodeFromJsonElement(DeclareTransactionV1Payload.serializer(), element)
            Felt(2).hexString() -> decoder.json.decodeFromJsonElement(DeclareTransactionV2Payload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid declare transaction version")
        }

    override fun serialize(encoder: Encoder, value: TransactionPayload) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is InvokeTransactionPayload -> Json.encodeToJsonElement(InvokeTransactionPayloadSerializer, value).jsonObject
            is DeclareTransactionV1Payload -> Json.encodeToJsonElement(DeclareTransactionV1PayloadSerializer, value).jsonObject
            is DeclareTransactionV2Payload -> Json.encodeToJsonElement(DeclareTransactionV2PayloadSerializer, value).jsonObject
            is DeployAccountTransactionPayload -> Json.encodeToJsonElement(DeployAccountTransactionPayloadSerializer, value).jsonObject
            else -> throw IllegalArgumentException("Invalid transaction payload type")
        }

        encoder.encodeJsonElement(jsonObject)
    }
}
