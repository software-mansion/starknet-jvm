package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed class TransactionPayload

@Serializable
data class InvokeFunctionPayload(
    @SerialName("function_invocation")
    val invocation: Call,

    @SerialName("signature")
    val signature: Signature,

    @SerialName("max_fee")
    val maxFee: Felt,

    @SerialName("version")
    val version: Felt,

    @SerialName("nonce")
    val nonce: Felt,
) : TransactionPayload()

data class DeployTransactionPayload(
    val contractDefinition: ContractDefinition,
    val salt: Felt,
    val constructorCalldata: Calldata,
    val version: Felt,
) : TransactionPayload()

data class DeclareTransactionPayload(
    val contractDefinition: ContractDefinition,
    val maxFee: Felt,
    val nonce: Felt,
    val signature: Signature,
    val version: Felt,
) : TransactionPayload()
