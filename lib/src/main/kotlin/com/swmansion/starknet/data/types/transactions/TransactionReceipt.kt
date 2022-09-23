package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MessageToL1
import com.swmansion.starknet.data.types.MessageToL2
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

enum class TransactionReceiptType {
    DECLARE, DEPLOY, INVOKE, PENDING, PENDING_INVOKE, GATEWAY
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionStatus {
    @JsonNames("PENDING", "NOT_RECEIVED", "RECEIVED")
    PENDING,

    @JsonNames("ACCEPTED_ON_L1")
    ACCEPTED_ON_L1,

    @JsonNames("ACCEPTED_ON_L2")
    ACCEPTED_ON_L2,

    @JsonNames("REJECTED")
    REJECTED,

    @JsonNames("UNKNOWN")
    UNKNOWN,
}

@Serializable
sealed class TransactionReceipt {
    abstract val hash: Felt
    abstract val actualFee: Felt
    abstract val isPending: Boolean
    abstract val type: TransactionReceiptType
    abstract val status: TransactionStatus

    fun isAccepted(): Boolean {
        return (status == TransactionStatus.ACCEPTED_ON_L1) || (status == TransactionStatus.ACCEPTED_ON_L2)
    }
}

@Serializable
sealed class ProcessedTransactionReceipt : TransactionReceipt() {
    abstract val blockHash: Felt?
    abstract val blockNumber: Int?
    override val isPending: Boolean = false
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
    val messagesToL1: List<MessageToL1>,

    @JsonNames("l1_to_l2_consumed_message")
    val messageToL2: MessageToL2? = null,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

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
data class InvokeTransactionReceipt(
    @JsonNames("messages_sent")
    val messagesSent: List<MessageToL1>,

    @JsonNames("l1_origin_message")
    val l1OriginMessage: MessageToL2? = null,

    @JsonNames("events")
    val events: List<Event>,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("status")
    override val status: TransactionStatus = TransactionStatus.UNKNOWN,

    @JsonNames("status_data")
    val rejectionReason: String? = null,

    override val type: TransactionReceiptType = TransactionReceiptType.INVOKE,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("status")
    override val status: TransactionStatus = TransactionStatus.UNKNOWN,

    @JsonNames("status_data")
    val rejectionReason: String? = null,

    override val type: TransactionReceiptType = TransactionReceiptType.DECLARE,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeployTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("status")
    override val status: TransactionStatus = TransactionStatus.UNKNOWN,

    @JsonNames("status_data")
    val rejectionReason: String? = null,

    override val type: TransactionReceiptType = TransactionReceiptType.DEPLOY,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    override val isPending: Boolean = true,

    override val type: TransactionReceiptType = TransactionReceiptType.PENDING,

    override val status: TransactionStatus = TransactionStatus.PENDING,
) : TransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingInvokeTransactionReceipt(
    @JsonNames("messages_sent")
    val messagesSent: List<MessageToL1>,

    @JsonNames("l1_origin_message")
    val l1OriginMessage: MessageToL2? = null,

    @JsonNames("events")
    val events: List<Event>,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    override val isPending: Boolean = true,

    override val type: TransactionReceiptType = TransactionReceiptType.PENDING_INVOKE,

    override val status: TransactionStatus = TransactionStatus.PENDING,
) : TransactionReceipt()
