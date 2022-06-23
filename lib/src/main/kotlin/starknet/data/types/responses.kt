@file:JvmName("Responses")

package starknet.data.types

import kotlinx.serialization.Serializable


@Serializable
sealed class Response

@Serializable
data class CallContractResponse(
    val result: List<String>
): Response()

data class GetCodeResponse(
    val bytecode: Array<String>,
    val abi: Abi
)

data class TransactionFailureReason(
    val code: String,
    val errorMessage: String
)
