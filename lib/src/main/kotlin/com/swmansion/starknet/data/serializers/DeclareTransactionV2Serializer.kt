package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.DeclareTransactionV2
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal object DeclareTransactionV2Serializer : KSerializer<DeclareTransactionV2> {
    override val descriptor = PrimitiveSerialDescriptor("DeclareTransactionV2", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeclareTransactionV2) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version.value.hexString())
            put("max_fee", value.maxFee.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("compiled_class_hash", value.compiledClassHash.hexString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeclareTransactionV2 {
        throw SerializationException("Class used for serialization only.")
    }
}
