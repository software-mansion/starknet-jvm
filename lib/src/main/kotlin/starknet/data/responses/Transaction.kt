package starknet.data.responses

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import starknet.data.types.Calldata
import starknet.data.types.Felt
import starknet.data.types.Signature

@Serializable
sealed class Transaction {
    abstract val hash: Felt
    abstract val signature: Signature
    abstract val maxFee: Felt
}

@Serializable
@SerialName("DEPLOY")
data class DeployTransaction @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("constructor_calldata", "calldata")
    val constructorCalldata: Calldata,

    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("signature")
    override val signature: Signature = emptyList(),

    @JsonNames("max_fee")
    override val maxFee: Felt = Felt.ZERO
) : Transaction()

@Serializable
@SerialName("INVOKE_FUNCTION")
data class InvokeTransaction @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("calldata")
    val calldata: Calldata,

    @JsonNames("entry_point_selector")
    val entryPointSelector: Felt,

    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("signature")
    override val signature: Signature,

    @JsonNames("max_fee")
    override val maxFee: Felt,
) : Transaction()

@Serializable
@SerialName("DECLARE")
data class DeclareTransaction @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("class_hash")
    val classHash: Felt,

    @JsonNames("sender_address")
    val senderAddress: Felt,

    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("signature")
    override val signature: Signature,

    @JsonNames("max_fee")
    override val maxFee: Felt,
) : Transaction()
