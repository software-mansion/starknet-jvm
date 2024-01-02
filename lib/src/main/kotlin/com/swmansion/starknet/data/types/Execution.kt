@file:JvmName("Execution")

package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toCalldata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias Calldata = List<Felt>
typealias PaymasterData = List<Felt>
typealias AccountDeploymentData = List<Felt>
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
        /**
         * Construct a Call object using any objects conforming to ConvertibleToCalldata as calldata
         * instead of plain Felts.
         *
         * For example:
         * ```
         * Call.fromCallArguments(
         *      Felt.fromHex("0x1234"),
         *      Felt.fromHex("0x111"),
         *      Collections.listOf(Uint256.fromHex("0x1394924"), Felt.ZERO)
         * );
         * ```
         *
         * @param contractAddress an address to be called
         * @param entrypoint a selector of the entrypoint to be called
         * @param arguments CallArguments to be used in a call
         *
         * @return a Call object
         */
        @JvmStatic
        fun fromCallArguments(contractAddress: Felt, entrypoint: Felt, arguments: CallArguments): Call {
            val calldata = arguments.toCalldata()
            return Call(contractAddress, entrypoint, calldata)
        }

        /**
         * Construct a Call object using any objects conforming to ConvertibleToCalldata as calldata
         * instead of plain Felts, using selector name.
         *
         * For example:
         * ```
         * Call.fromCallArguments(
         *      Felt.fromHex("0x1234"),
         *      "mySelector",
         *      Collections.listOf(Uint256.fromHex("0x1394924"), Felt.ZERO)
         * );
         *
         * @param contractAddress an address to be called
         * @param entrypoint a name of the entrypoint to be called
         * @param arguments CallArguments to be used in a call
         *
         * @return a Call object
         */
        @JvmStatic
        fun fromCallArguments(contractAddress: Felt, entrypoint: String, arguments: CallArguments): Call {
            return fromCallArguments(contractAddress, selectorFromName(entrypoint), arguments)
        }
    }
}

object AccountCalldataTransformer {
    @JvmSynthetic
    private fun callsToExecuteCalldataCairo1(calls: List<Call>): List<Felt> {
        val result = mutableListOf<Felt>()

        result.add(Felt(calls.size))

        for (call in calls) {
            result.add(call.contractAddress)
            result.add(call.entrypoint)
            result.add(Felt(call.calldata.size))
            result.addAll(call.calldata)
        }

        return result
    }

    @JvmSynthetic
    private fun callsToExecuteCalldataCairo0(calls: List<Call>): List<Felt> {
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

    @JvmStatic
    fun callsToExecuteCalldata(calls: List<Call>, cairoVersion: Felt = Felt.ZERO): List<Felt> {
        if (cairoVersion == Felt.ONE) {
            return callsToExecuteCalldataCairo1(calls)
        }

        return callsToExecuteCalldataCairo0(calls)
    }
}
