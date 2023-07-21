package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.GatewayMessageL1ToL2
import com.swmansion.starknet.data.types.GatewayMessageL2ToL1
import com.swmansion.starknet.data.types.RpcMessageL2ToL1
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

enum class TransactionReceiptType {
    DECLARE, DEPLOY, INVOKE, PENDING, PENDING_DEPLOY, GATEWAY, DEPLOY_ACCOUNT, L1_HANDLER
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionStatus {
    @JsonNames("PENDING", "RECEIVED")
    PENDING,

    @JsonNames("ACCEPTED_ON_L1")
    ACCEPTED_ON_L1,

    @JsonNames("ACCEPTED_ON_L2")
    ACCEPTED_ON_L2,

    @JsonNames("REJECTED")
    REJECTED,

    @JsonNames("UNKNOWN", "NOT_RECEIVED")
    UNKNOWN,
}

@Serializable
sealed class TransactionReceipt {
    abstract val hash: Felt
    abstract val actualFee: Felt?
    abstract val type: TransactionReceiptType
    abstract val status: TransactionStatus
    val isAccepted: Boolean
        get() = (status == TransactionStatus.ACCEPTED_ON_L1) || (status == TransactionStatus.ACCEPTED_ON_L2)
}

@Serializable
sealed class ProcessedTransactionReceipt : TransactionReceipt() {
    abstract val blockHash: Felt?
    abstract val blockNumber: Int?
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GatewayFailureReason(
    @JsonNames("error_message")
    val errorMessage: String?,

    @JsonNames("code")
    val code: String?,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GatewayTransactionReceipt(
    @JsonNames("events")
    val events: List<Event>,

    @JsonNames("l2_to_l1_messages")
    val messagesL2ToL1: List<GatewayMessageL2ToL1>,

    @JsonNames("l1_to_l2_consumed_message")
    val messageL1ToL2: GatewayMessageL1ToL2? = null,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt? = null,

    @JsonNames("block_number")
    override val blockNumber: Int? = null,

    @JsonNames("status")
    override val status: TransactionStatus = TransactionStatus.UNKNOWN,

    @JsonNames("transaction_failure_reason")
    val failureReason: GatewayFailureReason? = null,

    override val type: TransactionReceiptType = TransactionReceiptType.GATEWAY,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
// OptIn needed because @JsonNames is part of the experimental serialization api
data class RpcTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt?,

    @JsonNames("block_number")
    override val blockNumber: Int?,

    override val type: TransactionReceiptType,

    @JsonNames("messages_sent")
    val messagesSent: List<RpcMessageL2ToL1>,

    @JsonNames("events")
    val events: List<Event>,

) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeployRpcTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt?,

    @JsonNames("block_number")
    override val blockNumber: Int?,

    override val type: TransactionReceiptType,

    @JsonNames("messages_sent")
    val messagesSent: List<RpcMessageL2ToL1>,

    @JsonNames("events")
    val events: List<Event>,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingRpcTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    val messagesSent: List<RpcMessageL2ToL1>,

    @JsonNames("events")
    val events: List<Event>,

    override val type: TransactionReceiptType = TransactionReceiptType.PENDING,

    override val status: TransactionStatus = TransactionStatus.PENDING,
) : TransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingRpcDeployTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    val messagesSent: List<RpcMessageL2ToL1>,

    @JsonNames("events")
    val events: List<Event>,

    override val type: TransactionReceiptType = TransactionReceiptType.PENDING_DEPLOY,

    override val status: TransactionStatus = TransactionStatus.PENDING,

    @JsonNames("contract_address")
    val contractAddress: Felt? = null,
) : TransactionReceipt()
