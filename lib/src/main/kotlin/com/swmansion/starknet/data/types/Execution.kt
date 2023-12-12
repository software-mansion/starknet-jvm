@file:JvmName("Execution")

package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.data.types.transactions.DAMode
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
            val calldata = arguments.flatMap { it.toCalldata() }
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

data class ExecutionParams(
    val nonce: Felt,
    val maxFee: Felt,
)

sealed class ExecutionParamsV3 {
    abstract val nonce: Felt
    abstract val resourceBounds: ResourceBoundsMapping
    abstract val tip: Uint64
    abstract val paymasterData: PaymasterData
    abstract val nonceDataAvailabilityMode: DAMode
    abstract val feeDataAvailabilityMode: DAMode
}

data class InvokeExecutionParamsV3(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    val accountDeploymentData: AccountDeploymentData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ExecutionParamsV3() {
    constructor(nonce: Felt, l1ResourceBounds: ResourceBounds) : this(
        nonce = nonce,
        resourceBounds = ResourceBoundsMapping(l1ResourceBounds, ResourceBounds(Uint64.ZERO, Uint128.ZERO)),
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )
}

data class DeclareExecutionParamsV3(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    val accountDeploymentData: AccountDeploymentData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ExecutionParamsV3()

data class DeployAccountExecutionParamsV3(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ExecutionParamsV3()

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
