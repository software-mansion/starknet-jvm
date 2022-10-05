package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.DECLARE_SENDER_ADDRESS
import com.swmansion.starknet.data.types.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
)

data class DeployTransactionPayload(
    val contractDefinition: ContractDefinition,
    val salt: Felt,
    val constructorCalldata: Calldata,
    val version: Felt,
)

data class DeclareTransactionPayload(
    val contractDefinition: ContractDefinition,
    val senderAddress: Felt,
    val maxFee: Felt,
    val nonce: Felt,
    val signature: Signature,
    val version: Felt,
) {
    constructor(
        contractDefinition: ContractDefinition,
        maxFee: Felt,
        nonce: Felt,
        signature: Signature,
        version: Felt,
    ) : this(contractDefinition, DECLARE_SENDER_ADDRESS, maxFee, nonce, signature, version)
}
