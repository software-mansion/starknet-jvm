package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal val transactionIgnoredKeys = listOf("transaction_hash", "contract_address")

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
        require(value is ExecutableTransaction) {
            "TransactionSerializer can only serialize ExecutableTransaction instances."
        }

        val jsonObject = encoder.json.encodeToJsonElement(ExecutableTransactionSerializer, value).jsonObject
        val result = JsonObject(
            jsonObject.filter { (key, _) -> !transactionIgnoredKeys.contains(key) }.plus("type" to encoder.json.encodeToJsonElement(value.type)),
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
            Felt(0) -> decoder.json.decodeFromJsonElement(DeclareTransactionV0.serializer(), element)
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
