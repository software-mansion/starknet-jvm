package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.InvokeTransactionV1
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object InvokeTransactionV1Serializer : KSerializer<InvokeTransactionV1> {
    override val descriptor = PrimitiveSerialDescriptor("InvokeTransactionV1", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: InvokeTransactionV1) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
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
