package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.*

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
data class GetBlockHashAndNumberResponse(
    @JsonNames("block_hash")
    val blockHash: Felt,

    @JsonNames("block_number")
    val blockNumber: Int,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GetBlockNumberResponse(
    @JsonNames("block_number")
    val blockNumber: Int,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GetBlockTransactionCount(
    @JsonNames("transaction_receipts")
    @Serializable(UnwrappingJsonListSerializer::class) val transactionCount: Int,
)

object UnwrappingJsonListSerializer :
    JsonTransformingSerializer<Int>(Int.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) return element
        return JsonPrimitive(element.size)
    }
}
