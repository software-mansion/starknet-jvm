@file:JvmName("Params")

package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.transactions.DAMode

sealed class ParamsBase

data class ExecutionParams(
    val nonce: Felt,
    val maxFee: Felt,
) : ParamsBase()

sealed class ParamsV3 : ParamsBase() {
    abstract val nonce: Felt
    abstract val resourceBounds: ResourceBoundsMapping
    abstract val tip: Uint64
    abstract val paymasterData: PaymasterData
    abstract val nonceDataAvailabilityMode: DAMode
    abstract val feeDataAvailabilityMode: DAMode
}

data class ExecutionParamsV3(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    val accountDeploymentData: AccountDeploymentData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ParamsV3() {
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

data class DeclareParamsV3(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    val accountDeploymentData: AccountDeploymentData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ParamsV3()

data class DeployAccontParamsV3(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ParamsV3()
