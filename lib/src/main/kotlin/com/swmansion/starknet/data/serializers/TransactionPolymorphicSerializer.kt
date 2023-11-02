package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal object TransactionPolymorphicSerializer : JsonContentPolymorphicSerializer<Transaction>(Transaction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out Transaction> {
        val jsonElement = element.jsonObject
        val typeElement = jsonElement.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }

        val type = Json.decodeFromJsonElement(TransactionType.serializer(), typeElement)

        return when (type) {
            TransactionType.INVOKE -> selectInvokeDeserializer(element)
            TransactionType.DECLARE -> selectDeclareDeserializer(element)
            TransactionType.DEPLOY_ACCOUNT -> DeployAccountTransactionV1.serializer()
            TransactionType.DEPLOY -> DeployTransaction.serializer()
            TransactionType.L1_HANDLER -> L1HandlerTransaction.serializer()
        }
    }
    private fun selectInvokeDeserializer(element: JsonElement): DeserializationStrategy<out InvokeTransaction> {
        val jsonElement = element.jsonObject
        val versionElement = jsonElement.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = Json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt.ONE -> InvokeTransactionV1.serializer()
            Felt.ZERO -> InvokeTransactionV0.serializer()
            else -> throw IllegalArgumentException("Invalid invoke transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun selectDeclareDeserializer(element: JsonElement): DeserializationStrategy<out DeclareTransaction> {
        val jsonElement = element.jsonObject
        val versionElement = jsonElement.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = Json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt.ZERO -> DeclareTransactionV0.serializer()
            Felt.ONE -> DeclareTransactionV1.serializer()
            Felt(2) -> DeclareTransactionV2.serializer()
            else -> throw IllegalArgumentException("Invalid declare transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }
}
