package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.serializers.TransactionPayloadSerializer
import com.swmansion.starknet.data.types.*
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
    val version: Felt,

    @SerialName("nonce")
    val nonce: Felt,
) : InvokeTransactionPayload()

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
) : InvokeTransactionPayload()

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
    val version: Felt,

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
    val version: Felt,

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
    val version: Felt,

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
