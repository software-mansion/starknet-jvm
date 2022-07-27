package starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import starknet.data.selectorFromName

@Serializable
data class Call(
    @SerialName("contract_address") val contractAddress: Felt,
    val entrypoint: Felt,
    val calldata: Calldata,
) {
    constructor(contractAddress: Felt, entrypoint: String, calldata: Calldata) : this(
        contractAddress,
        selectorFromName(entrypoint),
        calldata
    )
}

data class CallExtraParams(
    val blockHashOrTag: BlockHashOrTag
)

data class ExecutionParams(
    val nonce: Felt,
    val maxFee: Felt,
    val version: Felt
)

@Serializable
data class CallContractPayload(
    val request: Call,
    @SerialName("block_hash") val blockHashOrTag: BlockHashOrTag
)

@Serializable
data class GetStorageAtPayload(
    @SerialName("contract_address") val contractAddress: Felt,
    @SerialName("key") val key: Felt,
    @SerialName("block_hash") val blockHashOrTag: BlockHashOrTag
)

@Serializable
data class GetTransactionByHashPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt
)

@Serializable
data class GetTransactionReceiptPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt
)

fun callsToExecuteCalldata(calls: List<Call>, nonce: Felt): List<Felt> {
    val wholeCalldata = mutableListOf<Felt>()
    val callArray = mutableListOf<Felt>()
    for (call in calls) {
        callArray.add(call.contractAddress) // to
        callArray.add(call.entrypoint) // selector
        callArray.add(Felt(wholeCalldata.size)) // offset
        callArray.add(Felt(call.calldata.size)) // len

        wholeCalldata.addAll(call.calldata)
    }

    return buildList {
        add(Felt(calls.size))
        addAll(callArray)
        add(Felt(wholeCalldata.size))
        addAll(wholeCalldata)
        add(nonce)
    }
}
