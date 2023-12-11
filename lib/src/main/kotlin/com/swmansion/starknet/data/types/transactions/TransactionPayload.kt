package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TransactionPayload() {
    @SerialName("type")
    abstract val type: TransactionType
}

@Serializable
sealed class InvokeTransactionPayload() : TransactionPayload()

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
    val version: Felt,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("type")
    override val type: TransactionType = TransactionType.INVOKE,
) : InvokeTransactionPayload() {

    constructor(senderAddress: Felt, calldata: Calldata, signature: Signature, maxFee: Felt, nonce: Felt) : this(
        senderAddress = senderAddress,
        calldata = calldata,
        signature = signature,
        maxFee = maxFee,
        version = Felt.ONE,
        nonce = nonce,
        type = TransactionType.INVOKE,
    )
}

@Serializable
data class InvokeTransactionV3Payload(
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
    val version: Felt,

    @SerialName("type")
    override val type: TransactionType = TransactionType.INVOKE,
) : InvokeTransactionPayload() {

    constructor(
        senderAddress: Felt,
        calldata: Calldata,
        signature: Signature,
        nonce: Felt,
        resourceBounds: ResourceBoundsMapping,
        tip: Uint64,
        paymasterData: List<Felt>,
        accountDeploymentData: List<Felt>,
        nonceDataAvailabilityMode: DAMode,
        feeDataAvailabilityMode: DAMode,
    ) : this(
        senderAddress = senderAddress,
        calldata = calldata,
        signature = signature,
        nonce = nonce,
        resourceBounds = resourceBounds,
        tip = tip,
        paymasterData = paymasterData,
        accountDeploymentData = accountDeploymentData,
        nonceDataAvailabilityMode = nonceDataAvailabilityMode,
        feeDataAvailabilityMode = feeDataAvailabilityMode,
        version = Felt(3),
        type = TransactionType.INVOKE,
    )
}

@Serializable
sealed class DeclareTransactionPayload() : TransactionPayload()

@Serializable
data class DeclareTransactionV1Payload(
    @SerialName("contract_class")
    val contractDefinition: Cairo0ContractDefinition,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("sender_address")
    val senderAddress: Felt,

    @SerialName("version")
    val version: Felt = Felt.ONE,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,
) : DeclareTransactionPayload()

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
    val version: Felt = Felt(2),

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,
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
    val version: Felt = Felt(3),

    @SerialName("type")
    override val type: TransactionType = TransactionType.DECLARE,
) : DeclareTransactionPayload()

@Serializable
sealed class DeployAccountTransactionPayload() : TransactionPayload()

@Serializable
data class DeployAccountTransactionV1Payload(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("contract_address_salt")
    val salt: Felt,

    @SerialName("constructor_calldata")
    val constructorCalldata: Calldata,

    @SerialName("version")
    val version: Felt,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,
) : DeployAccountTransactionPayload()

@Serializable
data class DeployAccountTransactionV3Payload(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("contract_address_salt")
    val salt: Felt,

    @SerialName("constructor_calldata")
    val constructorCalldata: Calldata,

    @SerialName("version")
    val version: Felt,

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

    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,
) : DeployAccountTransactionPayload()
