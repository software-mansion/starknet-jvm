@file:JvmName("Responses")

package starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
sealed class Response

@Serializable
data class CallContractResponse(
    val result: List<Felt>
) : Response()

@Serializable
data class InvokeFunctionResponse(
    @SerialName("transaction_hash") val transactionHash: Felt
) : Response()

@Serializable
data class DeployResponse @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("contract_address", "address")
    val contractAddress: Felt
)

@Serializable
data class DeclareResponse @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("class_hash")
    val classHash: Felt
)

@Serializable
data class GetStorageAtResponse(
    val result: Felt
) : Response()

data class TransactionFailureReason(
    val code: String,
    val errorMessage: String
)
