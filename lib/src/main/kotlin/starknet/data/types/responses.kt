@file:JvmName("Responses")

package starknet.data.types

import kotlinx.serialization.Serializable
import types.Felt

@Serializable
sealed class Response

@Serializable
data class CallContractResponse(
    val result: List<Felt>
): Response()

data class GetCodeResponse(
    val bytecode: List<String>,
    val abi: Abi
)

data class TransactionFailureReason(
    val code: String,
    val errorMessage: String
)
