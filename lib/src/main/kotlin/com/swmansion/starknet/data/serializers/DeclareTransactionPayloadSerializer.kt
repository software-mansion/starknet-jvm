package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.transactions.DeclareTransactionPayload
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object DeclareTransactionPayloadSerializer : KSerializer<DeclareTransactionPayload> {

    override val descriptor: SerialDescriptor
        get() = DeclareTransactionPayload.serializer().descriptor

    override fun serialize(encoder: Encoder, value: DeclareTransactionPayload) {
        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version)
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("type", value.type.toString())
        }

        val output = encoder as? JsonEncoder ?: throw SerializationException("")

        output.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionPayload {
        throw Exception("Class used for serialization only.")
    }
}
