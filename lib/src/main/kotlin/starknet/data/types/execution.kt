package starknet.data.types

import starknet.data.selectorFromName
import types.Felt

data class Call(
    val contractAddress: Felt,
    val entrypoint: String,
    val calldata: Calldata,
)

data class ExecutionParams(
    val nonce: Felt,
    val maxFee: Felt,
    val version: Felt = Felt.ZERO,
)

fun callsToExecuteCalldata(calls: List<Call>, nonce: Felt): List<Felt> {
    val wholeCalldata = mutableListOf<Felt>()
    val callArray = mutableListOf<Felt>()
    for (call in calls) {
        callArray.add(call.contractAddress) // to
        callArray.add(selectorFromName(call.entrypoint)) // selector
        callArray.add(Felt(wholeCalldata.size)) // offset
        callArray.add(Felt(call.calldata.size)) // len

        wholeCalldata.addAll(call.calldata)
    }

    return buildList() {
        add(Felt(calls.size))
        addAll(callArray)
        add(Felt(wholeCalldata.size))
        addAll(wholeCalldata)
        add(nonce)
    }
}