package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.TransactionPayloadSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = TransactionPayloadSerializer::class)
sealed class TransactionPayload {
    abstract val type: TransactionType
}

@Serializable
sealed class InvokeTransactionPayload : TransactionPayload() {
    @SerialName("type")
    override val type: TransactionType = TransactionType.INVOKE
}

@Serializable
data class InvokeTransactionV1Payload(
    @SerialName("sender_address")
    val senderAddress: Felt,

    @SerialName("calldata")
    val calldata: Calldata,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("version")
    val version: TransactionVersion,

    @SerialName("nonce")
    val nonce: Felt,
) : InvokeTransactionPayload()

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InvokeTransactionV3Payload private constructor(
    @SerialName("sender_address")
    val senderAddress: Felt,

    @SerialName("calldata")
    val calldata: Calldata,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("resource_bounds")
    val resourceBounds: ResourceBoundsMapping,

    @SerialName("tip")
    val tip: Uint64,

    @SerialName("paymaster_data")
    val paymasterData: List<Felt>,

    @SerialName("account_deployment_data")
    val accountDeploymentData: List<Felt>,

    @SerialName("nonce_data_availability_mode")
    val nonceDataAvailabilityMode: DAMode,

    @SerialName("fee_data_availability_mode")
    val feeDataAvailabilityMode: DAMode,

    @SerialName("version")
    val version: TransactionVersion,
) : InvokeTransactionPayload() {
    constructor(
        senderAddress: Felt,
        calldata: Calldata,
        signature: Signature,
        nonce: Felt,
        resourceBounds: ResourceBoundsMapping,
        version: TransactionVersion,
    ) : this(
        senderAddress = senderAddress,
        calldata = calldata,
        signature = signature,
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
        version = version,
    )
}

@Serializable
sealed class DeclareTransactionPayload : TransactionPayload() {
    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE
}

@Serializable
data class DeclareTransactionV2Payload(
    @SerialName("contract_class")
    val contractDefinition: Cairo1ContractDefinition,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("sender_address")
    val senderAddress: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,

    @SerialName("version")
    val version: TransactionVersion,
) : DeclareTransactionPayload()

@Serializable
data class DeclareTransactionV3Payload(
    @SerialName("contract_class")
    val contractDefinition: Cairo1ContractDefinition,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("sender_address")
    val senderAddress: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,

    @SerialName("resource_bounds")
    val resourceBounds: ResourceBoundsMapping,

    @SerialName("tip")
    val tip: Uint64,

    @SerialName("paymaster_data")
    val paymasterData: List<Felt>,

    @SerialName("account_deployment_data")
    val accountDeploymentData: List<Felt>,

    @SerialName("nonce_data_availability_mode")
    val nonceDataAvailabilityMode: DAMode,

    @SerialName("fee_data_availability_mode")
    val feeDataAvailabilityMode: DAMode,

    @SerialName("version")
    val version: TransactionVersion,
) : DeclareTransactionPayload() {
    constructor(
        contractDefinition: Cairo1ContractDefinition,
        nonce: Felt,
        signature: Signature,
        senderAddress: Felt,
        compiledClassHash: Felt,
        resourceBounds: ResourceBoundsMapping,
        version: TransactionVersion,
    ) : this(
        contractDefinition = contractDefinition,
        nonce = nonce,
        signature = signature,
        senderAddress = senderAddress,
        compiledClassHash = compiledClassHash,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        accountDeploymentData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
        version = version,
    )
}

@Serializable
sealed class DeployAccountTransactionPayload : TransactionPayload() {
    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT
}

@Serializable
data class DeployAccountTransactionV1Payload(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("contract_address_salt")
    val salt: Felt,

    @SerialName("constructor_calldata")
    val constructorCalldata: Calldata,

    @SerialName("version")
    val version: TransactionVersion,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("signature")
    val signature: Signature,
) : DeployAccountTransactionPayload()

@Suppress("DataClassPrivateConstructor")
@Serializable
data class DeployAccountTransactionV3Payload private constructor(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("contract_address_salt")
    val salt: Felt,

    @SerialName("constructor_calldata")
    val constructorCalldata: Calldata,

    @SerialName("version")
    val version: TransactionVersion,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("resource_bounds")
    val resourceBounds: ResourceBoundsMapping,

    @SerialName("tip")
    val tip: Uint64,

    @SerialName("paymaster_data")
    val paymasterData: List<Felt>,

    @SerialName("nonce_data_availability_mode")
    val nonceDataAvailabilityMode: DAMode,

    @SerialName("fee_data_availability_mode")
    val feeDataAvailabilityMode: DAMode,
) : DeployAccountTransactionPayload() {
    constructor(
        classHash: Felt,
        salt: Felt,
        constructorCalldata: Calldata,
        version: TransactionVersion,
        nonce: Felt,
        signature: Signature,
        resourceBounds: ResourceBoundsMapping,
    ) : this(
        classHash = classHash,
        salt = salt,
        constructorCalldata = constructorCalldata,
        version = version,
        nonce = nonce,
        signature = signature,
        resourceBounds = resourceBounds,
        tip = Uint64.ZERO,
        paymasterData = emptyList(),
        nonceDataAvailabilityMode = DAMode.L1,
        feeDataAvailabilityMode = DAMode.L1,
    )
}
