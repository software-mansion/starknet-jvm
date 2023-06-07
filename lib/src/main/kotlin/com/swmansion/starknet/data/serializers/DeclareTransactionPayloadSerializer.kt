package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.DeclareTransactionV1Payload
import com.swmansion.starknet.data.types.transactions.DeclareTransactionV2Payload
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object DeclareTransactionV1PayloadSerializer : KSerializer<DeclareTransactionV1Payload> {

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
