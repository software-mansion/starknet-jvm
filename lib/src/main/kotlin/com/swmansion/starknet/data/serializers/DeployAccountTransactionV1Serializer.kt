package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.DeployAccountTransactionV1
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object DeployAccountTransactionV1Serializer : KSerializer<DeployAccountTransactionV1> {
    override val descriptor = PrimitiveSerialDescriptor("DeployAccountTransactionV1", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeployAccountTransactionV1) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("class_hash", value.classHash.hexString())
            put("contract_address_salt", value.contractAddressSalt.hexString())
            put("constructor_calldata", Json.encodeToJsonElement(value.constructorCalldata))
            put("max_fee", value.maxFee.hexString())
            put("version", value.version.value.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeployAccountTransactionV1 {
        throw SerializationException("Class used for serialization only.")
    }
}
