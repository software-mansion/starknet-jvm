package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object TransactionPayloadSerializer : KSerializer<Transaction> {
    override val descriptor = PrimitiveSerialDescriptor("TransactionPayload", PrimitiveKind.STRING)

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
            else -> throw IllegalArgumentException("Invalid transaction type '${typeElement.jsonPrimitive.content}'")
        }
    }

    override fun serialize(encoder: Encoder, value: Transaction) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is InvokeTransactionV1 -> Json.encodeToJsonElement(InvokeTransactionV1.serializer(), value).jsonObject
            is InvokeTransactionV3 -> Json.encodeToJsonElement(InvokeTransactionV3.serializer(), value).jsonObject
            is DeclareTransactionV2 -> Json.encodeToJsonElement(DeclareTransactionV2.serializer(), value).jsonObject
            is DeclareTransactionV3 -> Json.encodeToJsonElement(DeclareTransactionV3.serializer()).jsonObject
            is DeployAccountTransactionV1 -> Json.encodeToJsonElement(DeployAccountTransactionV1.serializer(), value).jsonObject
            is DeployAccountTransactionV3 -> Json.encodeToJsonElement(DeployAccountTransactionV3.serializer(), value).jsonObject
            is DeclareTransactionV0 -> Json.encodeToJsonElement(DeclareTransactionV0.serializer(), value).jsonObject
            is DeclareTransactionV1 -> Json.encodeToJsonElement(DeclareTransactionV1.serializer(), value).jsonObject
            is DeployTransaction -> Json.encodeToJsonElement(DeployTransaction.serializer(), value).jsonObject
            is InvokeTransactionV0 -> Json.encodeToJsonElement(InvokeTransactionV0.serializer(), value).jsonObject
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
            else -> throw IllegalArgumentException("Invalid invoke transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun deserializeDeclare(decoder: JsonDecoder, element: JsonElement): DeclareTransaction {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(DeclareTransactionV3.serializer(), element)
            Felt(2) -> decoder.json.decodeFromJsonElement(DeclareTransactionV2.serializer(), element)
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

internal object InvokeTransactionV1Serializer : KSerializer<InvokeTransactionV1> {
    override val descriptor = PrimitiveSerialDescriptor("DeclareTransactionV2Payload", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InvokeTransactionV1) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("type", value.type.toString())
            put("calldata", Json.encodeToJsonElement(value.calldata))
            put("sender_address", value.senderAddress.hexString())
            put("max_fee", value.maxFee.hexString())
            put("version", value.version.value.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): InvokeTransactionV1 {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeclareTransactionV2PayloadSerializer : KSerializer<DeclareTransactionV2> {
    override val descriptor = PrimitiveSerialDescriptor("DeclareTransactionV2Payload", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeclareTransactionV2) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition!!.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version.value.hexString())
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("type", value.type.toString())
            put("compiled_class_hash", value.compiledClassHash.hexString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionV2 {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeclareTransactionV3PayloadSerializer : KSerializer<DeclareTransactionV3> {
    override val descriptor = PrimitiveSerialDescriptor("DeclareTransactionV3Payload", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeclareTransactionV3) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition!!.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version.value.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("type", value.type.toString())
            put("compiled_class_hash", value.compiledClassHash.hexString())
            put("resource_bounds", Json.encodeToJsonElement(value.resourceBounds))
            put("tip", value.tip.hexString())
            putJsonArray("paymaster_data") { value.paymasterData.forEach { add(it) } }
            putJsonArray("account_deployment_data") { value.accountDeploymentData.forEach { add(it) } }
            put("fee_data_availability_mode", value.feeDataAvailabilityMode.toString())
            put("nonce_data_availability_mode", value.nonceDataAvailabilityMode.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionV3 {
        throw SerializationException("Class used for serialization only.")
    }
}
