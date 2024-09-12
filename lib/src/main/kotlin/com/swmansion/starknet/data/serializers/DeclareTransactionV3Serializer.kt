package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.DeclareTransactionV3
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

internal object DeclareTransactionV3Serializer : KSerializer<DeclareTransactionV3> {
    override val descriptor = PrimitiveSerialDescriptor("DeclareTransactionV3", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DeclareTransactionV3) {
        require(encoder is JsonEncoder)

        val jsonObject = buildJsonObject {
            put("contract_class", value.contractDefinition!!.toJson())
            put("sender_address", value.senderAddress.hexString())
            put("version", value.version.value.hexString())
            putJsonArray("signature") { value.signature.forEach { add(it) } }
            put("nonce", value.nonce)
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
