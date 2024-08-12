package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.DeployAccountTransactionV3
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object DeployAccountTransactionV3Serializer : KSerializer<DeployAccountTransactionV3> {
    override val descriptor = PrimitiveSerialDescriptor("DeployAccountTransactionV3", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeployAccountTransactionV3) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("class_hash", value.classHash.hexString())
            put("contract_address_salt", value.contractAddressSalt.hexString())
            put("constructor_calldata", Json.encodeToJsonElement(value.constructorCalldata))
            put("version", value.version.value.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
            put("resource_bounds", Json.encodeToJsonElement(value.resourceBounds))
            put("tip", value.tip.hexString())
            putJsonArray("paymaster_data") { value.paymasterData.forEach { add(it) } }
            put("nonce_data_availability_mode", value.nonceDataAvailabilityMode.toString())
            put("fee_data_availability_mode", value.feeDataAvailabilityMode.toString())
        }

        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): DeployAccountTransactionV3 {
        throw SerializationException("Class used for serialization only.")
    }
}
