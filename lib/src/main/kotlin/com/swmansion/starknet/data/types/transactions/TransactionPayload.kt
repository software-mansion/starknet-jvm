package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvokeTransactionPayload(

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

){
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

data class DeployTransactionPayload(
    val contractDefinition: ContractDefinition,
    val salt: Felt,
    val constructorCalldata: Calldata,
    val version: Felt,
    val type: TransactionType = TransactionType.DEPLOY,
)

data class DeclareTransactionPayload(
    val contractDefinition: ContractDefinition,
    val maxFee: Felt,
    val nonce: Felt,
    val signature: Signature,
    val version: Felt,
    val senderAddress: Felt,
    val type: TransactionType = TransactionType.DECLARE,
)

@Serializable
data class DeployAccountTransactionPayload(
    val classHash: Felt,
    val salt: Felt,
    val constructorCalldata: Calldata,
    val version: Felt,
    val nonce: Felt,
    val maxFee: Felt,
    val signature: Signature,
    val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,
)
