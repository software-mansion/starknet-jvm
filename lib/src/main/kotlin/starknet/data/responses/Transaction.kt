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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DEPLOY")
// OptIn needed because @JsonNames is part of the experimental serialization api
data class DeployTransaction(
    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("constructor_calldata", "calldata")
    val constructorCalldata: Calldata,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("signature")
    override val signature: Signature = emptyList(),

    @JsonNames("max_fee")
    override val maxFee: Felt = Felt.ZERO,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("INVOKE_FUNCTION")
data class InvokeTransaction(
    @JsonNames("contract_address")
    val contractAddress: Felt,

    @JsonNames("calldata")
    val calldata: Calldata,

    @JsonNames("entry_point_selector")
    val entryPointSelector: Felt,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("signature")
    override val signature: Signature,

    @JsonNames("max_fee")
    override val maxFee: Felt,
) : Transaction()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("DECLARE")
data class DeclareTransaction(
    @JsonNames("class_hash", "contract_class")
    val classHash: Felt,

    @JsonNames("sender_address")
    val senderAddress: Felt,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("signature")
    override val signature: Signature,

    @JsonNames("max_fee")
    override val maxFee: Felt,
) : Transaction()
