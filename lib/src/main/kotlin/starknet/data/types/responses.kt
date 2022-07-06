@file:JvmName("Responses")

package starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import types.Felt

@Serializable
sealed class Response

@Serializable
data class CallContractResponse(
    val result: List<Felt>
): Response()

@Serializable
data class InvokeFunctionResponse(
    @SerialName("transaction_hash") val transactionHash: Felt
): Response()

@Serializable
data class GetStorageAtResponse(
    val result: Felt
): Response()

data class GetCodeResponse(
    val bytecode: List<String>,
    val abi: Abi
)

data class TransactionFailureReason(
    val code: String,
    val errorMessage: String
)
