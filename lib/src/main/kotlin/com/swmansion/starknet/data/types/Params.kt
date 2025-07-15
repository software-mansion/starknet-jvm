@file:JvmName("Params")

package com.swmansion.starknet.data.types

/**
 * Params used for signing and sending transactions.
 */
sealed class ParamsV3 {
    abstract val nonce: Felt
    abstract val resourceBounds: ResourceBoundsMapping
    abstract val tip: Uint64
    abstract val paymasterData: PaymasterData
    abstract val nonceDataAvailabilityMode: DAMode
    abstract val feeDataAvailabilityMode: DAMode
}

/**
 * Params used for signing and sending v3 invoke transactions.
 */
// TODO: Make primary constructor public once values are no longer hardcoded on Starknet
@Suppress("DataClassPrivateConstructor")
data class InvokeParamsV3 private constructor(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    val accountDeploymentData: AccountDeploymentData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ParamsV3() {
    constructor(nonce: Felt, resourceBounds: ResourceBoundsMapping, tip: Uint64) : this(
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = tip,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )

    constructor(nonce: Felt, resourceBounds: ResourceBoundsMapping) : this(
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )
}

/**
 * Params used for signing and sending v3 declare transactions.
 */
// TODO: Make primary constructor public once values are no longer hardcoded on Starknet
@Suppress("DataClassPrivateConstructor")
data class DeclareParamsV3 private constructor(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    val accountDeploymentData: AccountDeploymentData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ParamsV3() {
    constructor(nonce: Felt, resourceBounds: ResourceBoundsMapping) : this(
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )

    constructor(nonce: Felt, resourceBounds: ResourceBoundsMapping, tip: Uint64) : this(
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = tip,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )
}

/**
 * Params used for signing and sending v3 deploy account transactions.
 */
// TODO: Make primary constructor public once values are no longer hardcoded on Starknet
@Suppress("DataClassPrivateConstructor")
data class DeployAccountParamsV3 private constructor(
    override val nonce: Felt,
    override val resourceBounds: ResourceBoundsMapping,
    override val tip: Uint64,
    override val paymasterData: PaymasterData,
    override val nonceDataAvailabilityMode: DAMode,
    override val feeDataAvailabilityMode: DAMode,
) : ParamsV3() {
    @JvmOverloads
    constructor(
        nonce: Felt = Felt.ZERO,
        resourceBounds: ResourceBoundsMapping,
        tip: Uint64 = Uint64.ZERO,
    ) : this(
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = tip,
        paymasterData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )
}
