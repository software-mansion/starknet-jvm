package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal object TransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Transaction> {
        val jsonElement = element.jsonObject
        val typeElement = jsonElement.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }

        val type = Json.decodeFromJsonElement(TransactionType.serializer(), typeElement)

        return when (type) {
            TransactionType.INVOKE -> selectInvokeDeserializer(element)
            TransactionType.DECLARE -> selectDeclareDeserializer(element)
            TransactionType.DEPLOY_ACCOUNT -> selectDeployAccountDeserializer(element)
            TransactionType.DEPLOY -> DeployTransaction.serializer()
            TransactionType.L1_HANDLER -> L1HandlerTransaction.serializer()
        }
    }
    private fun selectInvokeDeserializer(element: JsonElement): DeserializationStrategy<InvokeTransaction> {
        val jsonElement = element.jsonObject
        val versionElement = jsonElement.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = Json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> InvokeTransactionV3.serializer()
            Felt.ONE -> InvokeTransactionV1.serializer()
            Felt.ZERO -> InvokeTransactionV0.serializer()
            else -> throw IllegalArgumentException("Invalid invoke transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun selectDeployAccountDeserializer(element: JsonElement): DeserializationStrategy<DeployAccountTransaction> {
        val jsonElement = element.jsonObject
        val versionElement = jsonElement.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = Json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> DeployAccountTransactionV3.serializer()
            Felt.ONE -> DeployAccountTransactionV1.serializer()
            else -> throw IllegalArgumentException("Invalid deploy account transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun selectDeclareDeserializer(element: JsonElement): DeserializationStrategy<DeclareTransaction> {
        val jsonElement = element.jsonObject
        val versionElement = jsonElement.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = Json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> DeclareTransactionV3.serializer()
            Felt(2) -> DeclareTransactionV2.serializer()
            Felt.ONE -> DeclareTransactionV1.serializer()
            Felt.ZERO -> DeclareTransactionV0.serializer()
            else -> throw IllegalArgumentException("Invalid declare transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }
}
