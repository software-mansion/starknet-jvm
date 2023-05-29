package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.serializers.JsonRpcTransactionPayloadPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = JsonRpcTransactionPayloadPolymorphicSerializer::class)
sealed class TransactionPayload

@Serializable
data class InvokeTransactionPayload constructor(

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
) : TransactionPayload() {
    @SerialName("type")
    val type: TransactionType = TransactionType.INVOKE

    constructor(senderAddress: Felt, calldata: Calldata, signature: Signature, maxFee: Felt, nonce: Felt) : this(
        senderAddress,
        calldata,
        signature,
        maxFee,
        INVOKE_VERSION,
        nonce,
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
) : DeclareTransactionPayload() {
    @SerialName("type")
    val type: TransactionType = TransactionType.DECLARE
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
    val version: Felt = Felt(2),
) : DeclareTransactionPayload() {
    @SerialName("type")
    val type: TransactionType = TransactionType.DECLARE
}

@Serializable
data class DeployAccountTransactionPayload(
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
) : TransactionPayload() {
    @SerialName("type")
    val type: TransactionType = TransactionType.DEPLOY_ACCOUNT
}
