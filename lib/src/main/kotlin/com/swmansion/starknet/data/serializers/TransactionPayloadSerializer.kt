package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.DeclareTransactionV1Payload
import com.swmansion.starknet.data.types.transactions.DeclareTransactionV2Payload
import com.swmansion.starknet.data.types.transactions.DeployAccountTransactionPayload
import com.swmansion.starknet.data.types.transactions.InvokeTransactionPayload
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
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

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
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

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
internal object InvokeTransactionPayloadSerializer : KSerializer<InvokeTransactionPayload> {

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

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
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
