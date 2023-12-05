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
            TransactionType.INVOKE -> decoder.json.decodeFromJsonElement(InvokeTransactionPayload.serializer(), element)
            TransactionType.DECLARE -> deserializeDeclare(decoder, element)
            TransactionType.DEPLOY_ACCOUNT -> decoder.json.decodeFromJsonElement(DeployAccountTransactionPayload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid transaction type '${typeElement.jsonPrimitive.content}'")
        }
    }

    private fun deserializeDeclare(decoder: JsonDecoder, element: JsonElement): DeclareTransactionPayload {
        val versionElement = element.jsonObject.getOrElse("version") { throw SerializationException("Input element does not contain mandatory field 'version'") }

        val version = decoder.json.decodeFromJsonElement(Felt.serializer(), versionElement)
        return when (version) {
            Felt.ONE -> decoder.json.decodeFromJsonElement(DeclareTransactionV1Payload.serializer(), element)
            Felt(2) -> decoder.json.decodeFromJsonElement(DeclareTransactionV2Payload.serializer(), element)
            else -> throw IllegalArgumentException("Invalid declare transaction version '${versionElement.jsonPrimitive.content}'")
        }
    }

    override fun serialize(encoder: Encoder, value: TransactionPayload) {
        require(encoder is JsonEncoder)

        val jsonObject = when (value) {
            is InvokeTransactionPayload -> Json.encodeToJsonElement(InvokeTransactionPayloadSerializer, value).jsonObject
            is DeclareTransactionV1Payload -> Json.encodeToJsonElement(DeclareTransactionV1PayloadSerializer, value).jsonObject
            is DeclareTransactionV2Payload -> Json.encodeToJsonElement(DeclareTransactionV2PayloadSerializer, value).jsonObject
            is DeployAccountTransactionPayload -> Json.encodeToJsonElement(DeployAccountTransactionPayloadSerializer, value).jsonObject
            else -> throw IllegalArgumentException("Invalid transaction payload type [${value.type}]")
        }

        encoder.encodeJsonElement(jsonObject)
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

@OptIn(ExperimentalSerializationApi::class)
object DeclareTransactionV2PayloadSerializer : KSerializer<DeclareTransactionV2Payload> {

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

object InvokeTransactionPayloadSerializer : KSerializer<InvokeTransactionPayload> {

    override val descriptor: SerialDescriptor
        get() = DeclareTransactionV1Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: InvokeTransactionPayload) {
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

    override fun deserialize(decoder: Decoder): InvokeTransactionPayload {
        throw SerializationException("Class used for serialization only.")
    }
}

internal object DeployAccountTransactionPayloadSerializer : KSerializer<DeployAccountTransactionPayload> {
    override val descriptor: SerialDescriptor
        get() = DeclareTransactionV1Payload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeployAccountTransactionPayload) {
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

    override fun deserialize(decoder: Decoder): DeployAccountTransactionPayload {
        throw SerializationException("Class used for serialization only.")
    }
}
