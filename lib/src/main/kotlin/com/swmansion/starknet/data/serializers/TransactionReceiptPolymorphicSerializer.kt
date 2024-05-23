package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

internal object TransactionReceiptPolymorphicSerializer :
    JsonContentPolymorphicSerializer<TransactionReceipt>(TransactionReceipt::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<TransactionReceipt> {
        val jsonElement = element.jsonObject
        val typeElement = jsonElement.getOrElse("type") { throw SerializationException("Input element does not contain mandatory field 'type'") }

        val type = Json.decodeFromJsonElement(TransactionType.serializer(), typeElement)

        return when (type) {
            TransactionType.INVOKE -> InvokeTransactionReceipt.serializer()
            TransactionType.DEPLOY_ACCOUNT -> DeployAccountTransactionReceipt.serializer()
            TransactionType.DECLARE -> DeclareTransactionReceipt.serializer()
            TransactionType.L1_HANDLER -> L1HandlerTransactionReceipt.serializer()
            TransactionType.DEPLOY -> DeployTransactionReceipt.serializer()
        }
    }
}
