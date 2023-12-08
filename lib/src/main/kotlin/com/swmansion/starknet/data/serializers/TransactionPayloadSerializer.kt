package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = TransactionPayload::class)
internal object TransactionPayloadSerializer : KSerializer<TransactionPayload> {

    override val descriptor: SerialDescriptor
        get() = TransactionPayload.serializer().descriptor

    override fun deserialize(decoder: Decoder): TransactionPayload {
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

    override fun serialize(encoder: Encoder, value: TransactionPayload) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is InvokeTransactionV1Payload -> Json.encodeToJsonElement(InvokeTransactionPayloadV1Serializer, value).jsonObject
            is InvokeTransactionV3Payload -> Json.encodeToJsonElement(InvokeTransactionPayloadV3Serializer, value).jsonObject
            is DeclareTransactionV1Payload -> Json.encodeToJsonElement(DeclareTransactionV1PayloadSerializer, value).jsonObject
            is DeclareTransactionV2Payload -> Json.encodeToJsonElement(DeclareTransactionV2PayloadSerializer, value).jsonObject
            is DeclareTransactionV3Payload -> Json.encodeToJsonElement(DeclareTransactionV3PayloadSerializer, value).jsonObject
            is DeployAccountTransactionV1Payload -> Json.encodeToJsonElement(DeployAccountTransactionV1PayloadSerializer, value).jsonObject
            is DeployAccountTransactionV3Payload -> Json.encodeToJsonElement(DeployAccountTransactionV3PayloadSerializer, value).jsonObject
        }

        encoder.encodeJsonElement(jsonObject)
    }

    private fun deserializeInvoke(decoder: JsonDecoder, element: JsonElement): InvokeTransactionPayload {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(InvokeTransactionV3Payload.serializer(), element)
            Felt.ONE -> decoder.json.decodeFromJsonElement(InvokeTransactionV1Payload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid invoke transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun deserializeDeclare(decoder: JsonDecoder, element: JsonElement): DeclareTransactionPayload {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(DeclareTransactionV3Payload.serializer(), element)
            Felt(2) -> decoder.json.decodeFromJsonElement(DeclareTransactionV2Payload.serializer(), element)
            Felt.ONE -> decoder.json.decodeFromJsonElement(DeclareTransactionV1Payload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid declare transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    private fun deserializeDeployAccount(decoder: JsonDecoder, element: JsonElement): DeployAccountTransactionPayload {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt(3) -> decoder.json.decodeFromJsonElement(DeployAccountTransactionV3Payload.serializer(), element)
            Felt.ONE -> decoder.json.decodeFromJsonElement(DeployAccountTransactionV1Payload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid deploy account transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }
}

internal object DeclareTransactionV1PayloadSerializer : KSerializer<DeclareTransactionV1Payload> {

    override val descriptor: SerialDescriptor
        get() = DeclareTransactionV1Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeclareTransactionV1Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version)
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("type", value.type.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionV1Payload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeclareTransactionV2PayloadSerializer : KSerializer<DeclareTransactionV2Payload> {

    override val descriptor: SerialDescriptor
        get() = DeclareTransactionV2Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeclareTransactionV2Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version)
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("type", value.type.toString())
            put("compiled_class_hash", value.compiledClassHash.hexString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionV2Payload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeclareTransactionV3PayloadSerializer : KSerializer<DeclareTransactionV3Payload> {

    override val descriptor: SerialDescriptor
        get() = DeclareTransactionV3Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeclareTransactionV3Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version)
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("type", value.type.toString())
            put("compiled_class_hash", value.compiledClassHash.hexString())
            put("tip", value.tip.hexString())
            putJsonArray("paymaster_data") { value.paymasterData.forEach { add(it) } }
            putJsonArray("account_deployment_data") { value.accountDeploymentData.forEach { add(it) } }
            put("fee_data_availability_mode", value.feeDataAvailabilityMode.toString())
            put("nonce_data_availability_mode", value.nonceDataAvailabilityMode.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionV3Payload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object InvokeTransactionPayloadV1Serializer : KSerializer<InvokeTransactionV1Payload> {
    override val descriptor: SerialDescriptor
        get() = InvokeTransactionV1Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: InvokeTransactionV1Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("sender_address", value.senderAddress.hexString())
            putJsonArray("calldata") { value.calldata.forEach { add(it) } }
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("max_fee", value.maxFee.hexString())
            put("version", value.version)
            put("nonce", value.nonce)
            put("type", value.type.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): InvokeTransactionV1Payload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object InvokeTransactionPayloadV3Serializer : KSerializer<InvokeTransactionV3Payload> {
    override val descriptor: SerialDescriptor
        get() = InvokeTransactionV3Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: InvokeTransactionV3Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("sender_address", value.senderAddress.hexString())
            putJsonArray("calldata") { value.calldata.forEach { add(it) } }
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("max_fee", value.maxFee.hexString())
            put("version", value.version)
            put("nonce", value.nonce)
            put("resource_bounds", Json.encodeToJsonElement(value.resourceBounds))
            put("tip", value.tip.hexString())
            putJsonArray("paymaster_data") { value.paymasterData.forEach { add(it) } }
            putJsonArray("account_deployment_data") { value.accountDeploymentData.forEach { add(it) } }
            put("fee_data_availability_mode", value.feeDataAvailabilityMode.toString())
            put("nonce_data_availability_mode", value.nonceDataAvailabilityMode.toString())
            put("type", value.type.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): InvokeTransactionV3Payload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeployAccountTransactionV1PayloadSerializer : KSerializer<DeployAccountTransactionV1Payload> {
    override val descriptor: SerialDescriptor
        get() = DeployAccountTransactionV1Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeployAccountTransactionV1Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("class_hash", value.classHash.hexString())
            put("contract_address_salt", value.salt.hexString())
            putJsonArray("constructor_calldata") { value.constructorCalldata.forEach { add(it) } }
            put("version", value.version)
            put("nonce", value.nonce)
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("type", value.type.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeployAccountTransactionV1Payload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeployAccountTransactionV3PayloadSerializer : KSerializer<DeployAccountTransactionV3Payload> {
    override val descriptor: SerialDescriptor
        get() = DeployAccountTransactionV3Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeployAccountTransactionV3Payload) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("class_hash", value.classHash.hexString())
            put("contract_address_salt", value.salt.hexString())
            putJsonArray("constructor_calldata") { value.constructorCalldata.forEach { add(it) } }
            put("version", value.version)
            put("nonce", value.nonce)
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("resource_bounds", Json.encodeToJsonElement(value.resourceBounds))
            put("tip", value.tip.hexString())
            putJsonArray("paymaster_data") { value.paymasterData.forEach { add(it) } }
            put("fee_data_availability_mode", value.feeDataAvailabilityMode.toString())
            put("nonce_data_availability_mode", value.nonceDataAvailabilityMode.toString())
            put("type", value.type.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeployAccountTransactionV3Payload {
        throw SerializationException("Class used for serialization only.")
    }
}
