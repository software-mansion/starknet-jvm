@file:JvmName("Responses")

package starknet.types

data class CallContractResponse(
    val result: Array<String>
)

//data class GetBlockResponse(
//
//)

data class TransactionFailureReason(
    val code: String,
    val errorMessage: String
)

data class GetCodeResponse(
    val bytecode: Array<String>,
    val abi: Abi
)
