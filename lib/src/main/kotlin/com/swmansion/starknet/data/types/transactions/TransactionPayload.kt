package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.serializers.JsonRpcTransactionPayloadPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = JsonRpcTransactionPayloadPolymorphicSerializer::class)
sealed class TransactionPayload

@OptIn(ExperimentalSerializationApi::class)
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
    @EncodeDefault
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
data class DeclareTransactionPayload(
    @SerialName("contract_class")
    val contractDefinition: ContractDefinition,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("version")
    val version: Felt,

    @SerialName("sender_address")
    val senderAddress: Felt,
) : TransactionPayload() {
    @SerialName("type")
    val type: TransactionType = TransactionType.DECLARE
}

@OptIn(ExperimentalSerializationApi::class)
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
    @EncodeDefault
    @SerialName("type")
    val type: TransactionType = TransactionType.DEPLOY_ACCOUNT
}
