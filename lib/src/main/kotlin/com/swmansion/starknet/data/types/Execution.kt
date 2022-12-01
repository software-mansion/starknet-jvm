@file:JvmName("Execution")

package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias Calldata = List<Felt>
typealias Signature = List<Felt>
typealias CallArguments = List<ConvertibleToCalldata>

@Serializable
data class Call(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("entry_point_selector")
    val entrypoint: Felt,

    @SerialName("calldata")
    val calldata: Calldata,
) {
    constructor(contractAddress: Felt, entrypoint: String, calldata: Calldata) : this(
        contractAddress,
        selectorFromName(entrypoint),
        calldata,
    )

    constructor(contractAddress: Felt, entrypoint: Felt) : this(
        contractAddress,
        entrypoint,
        emptyList(),
    )

    constructor(contractAddress: Felt, entrypoint: String) : this(
        contractAddress,
        entrypoint,
        emptyList(),
    )

    companion object {
        @JvmStatic
        fun fromCallArguments(contractAddress: Felt, entrypoint: Felt, arguments: CallArguments): Call {
            val calldata = arguments.flatMap { it.toCalldata() }
            return Call(contractAddress, entrypoint, calldata)
        }

        @JvmStatic
        fun fromCallArguments(contractAddress: Felt, entrypoint: String, arguments: CallArguments): Call {
            return fromCallArguments(contractAddress, selectorFromName(entrypoint), arguments)
        }
    }
}

data class ExecutionParams(
    val nonce: Felt,
    val maxFee: Felt,
)

@JvmSynthetic
internal fun callsToExecuteCalldata(calls: List<Call>): List<Felt> {
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
    }
}
