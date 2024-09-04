package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object TransactionSerializer : KSerializer<Transaction> {
    override val descriptor = PrimitiveSerialDescriptor("Transaction", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Transaction {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        val jsonElement = element.jsonObject
        val typeElement = jsonElement.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }

        val type = decoder.json.decodeFromJsonElement(TransactionType.serializer(), typeElement)

        return when (type) {
            TransactionType.INVOKE -> deserializeInvoke(decoder, element)
            TransactionType.DECLARE -> deserializeDeclare(decoder, element)
            TransactionType.DEPLOY_ACCOUNT -> deserializeDeployAccount(decoder, element)
            TransactionType.DEPLOY -> Json.decodeFromJsonElement(DeployTransaction.serializer(), element)
            TransactionType.L1_HANDLER -> Json.decodeFromJsonElement(L1HandlerTransaction.serializer(), element)
        }
    }

    override fun serialize(encoder: Encoder, value: Transaction) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is InvokeTransactionV3 -> Json.encodeToJsonElement(InvokeTransactionV3Serializer, value).jsonObject
            is InvokeTransactionV1 -> Json.encodeToJsonElement(InvokeTransactionV1Serializer, value).jsonObject
            is InvokeTransactionV0 -> Json.encodeToJsonElement(InvokeTransactionV0.serializer(), value).jsonObject
            is DeclareTransactionV3 -> Json.encodeToJsonElement(DeclareTransactionV3Serializer, value).jsonObject
            is DeclareTransactionV2 -> Json.encodeToJsonElement(DeclareTransactionV2Serializer, value).jsonObject
            is DeclareTransactionV1 -> Json.encodeToJsonElement(DeclareTransactionV1.serializer(), value).jsonObject
            is DeclareTransactionV0 -> Json.encodeToJsonElement(DeclareTransactionV0.serializer(), value).jsonObject
            is DeployAccountTransactionV3 -> Json.encodeToJsonElement(DeployAccountTransactionV3Serializer, value).jsonObject
            is DeployAccountTransactionV1 -> Json.encodeToJsonElement(DeployAccountTransactionV1Serializer, value).jsonObject
            is DeployTransaction -> Json.encodeToJsonElement(DeployTransaction.serializer(), value).jsonObject
            is L1HandlerTransaction -> Json.encodeToJsonElement(L1HandlerTransaction.serializer(), value).jsonObject
        }

        val result = JsonObject(
            jsonObject.plus("type" to Json.encodeToJsonElement(value.type)),
        )

        encoder.encodeJsonElement(result)
    }

    private fun deserializeInvoke(decoder: JsonDecoder, element: JsonElement): InvokeTransaction {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(InvokeTransactionV3.serializer(), element)
            Felt.ONE -> decoder.json.decodeFromJsonElement(InvokeTransactionV1.serializer(), element)
            Felt.ZERO -> decoder.json.decodeFromJsonElement(InvokeTransactionV0.serializer(), element)
            else -> throw IllegalArgumentException("Invalid invoke transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun deserializeDeclare(decoder: JsonDecoder, element: JsonElement): DeclareTransaction {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(DeclareTransactionV3.serializer(), element)
            Felt(2) -> decoder.json.decodeFromJsonElement(DeclareTransactionV2.serializer(), element)
            Felt.ONE -> decoder.json.decodeFromJsonElement(DeclareTransactionV1.serializer(), element)
            Felt.ZERO -> decoder.json.decodeFromJsonElement(DeclareTransactionV0.serializer(), element)
            else -> throw IllegalArgumentException("Invalid declare transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun deserializeDeployAccount(decoder: JsonDecoder, element: JsonElement): DeployAccountTransaction {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(DeployAccountTransactionV3.serializer(), element)
            Felt.ONE -> decoder.json.decodeFromJsonElement(DeployAccountTransactionV1.serializer(), element)
            else -> throw IllegalArgumentException("Invalid deploy account transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }
}
