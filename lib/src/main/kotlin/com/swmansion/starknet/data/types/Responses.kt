package com.swmansion.starknet.data.types

import com.swmansion.starknet.extensions.toFelt
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.*
import java.math.BigInteger

@Serializable
data class CallContractResponse(
    val result: List<Felt>,
)

@Serializable
data class InvokeFunctionResponse(
    @SerialName("transaction_hash") val transactionHash: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
// OptIn needed because @JsonNames is part of the experimental serialization api
data class DeployResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("contract_address", "address")
    val contractAddress: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("class_hash")
    val classHash: Felt,
)

@Serializable
data class GetStorageAtResponse(
    val result: Felt,
)

data class TransactionFailureReason(
    val code: String,
    val errorMessage: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EstimateFeeResponse(
    @JsonNames("gas_consumed", "gas_usage")
    val gasConsumed: Felt,

    @JsonNames("gas_price")
    val gasPrice: Felt,

    @JsonNames("overall_fee")
    val overallFee: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = EstimateFeeResponse::class)
class EstimateFeeResponseGatewaySerializer: KSerializer<EstimateFeeResponse> {
    override fun deserialize(decoder: Decoder): EstimateFeeResponse {
        val input = decoder as? JsonDecoder ?: throw SerializationException("Expected JsonInput for ${decoder::class}")

        val jsonObject = input.decodeJsonElement().jsonObject

        val gasUsage = jsonObject.getValue("gas_usage").jsonPrimitive.content.toBigInteger().toFelt
        val gasPrice = jsonObject.getValue("gas_price").jsonPrimitive.content.toBigInteger().toFelt
        val overallFee = jsonObject.getValue("overall_fee").jsonPrimitive.content.toBigInteger().toFelt

        return EstimateFeeResponse(
            gasConsumed = gasUsage,
            gasPrice = gasPrice,
            overallFee = overallFee
        )
    }

    override val descriptor: SerialDescriptor
        get() = EstimateFeeResponse.serializer().descriptor

    override fun serialize(encoder: Encoder, value: EstimateFeeResponse) {
        TODO("Not implemented yet")
//        val jsonObject = buildJsonObject {
//            put("gas_usage", value.gasConsumed.value.toString(10))
//        }
    }
}