package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MessageL1ToL2
import com.swmansion.starknet.data.types.MessageL2ToL1
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

    @JsonNames("REVERTED")
    REVERTED,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionExecutionStatus {
    @JsonNames("SUCCEEDED")
    SUCCEEDED,

    @JsonNames("REVERTED")
    REVERTED,

    @JsonNames("REJECTED")
    REJECTED,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionFinalityStatus {
    @JsonNames("ACCEPTED_ON_L1")
    ACCEPTED_ON_L1,

    @JsonNames("ACCEPTED_ON_L2")
    ACCEPTED_ON_L2,

    @JsonNames("RECEIVED", "PENDING")
    RECEIVED,

    @JsonNames("NOT_RECEIVED", "UNKNOWN")
    NOT_RECEIVED,
}

@Serializable
sealed class TransactionReceipt {
    abstract val hash: Felt
    abstract val actualFee: Felt?
    abstract val type: TransactionReceiptType
    abstract val executionStatus: TransactionExecutionStatus
    abstract val finalityStatus: TransactionFinalityStatus
    abstract val revertReason: String?
    abstract val events: List<Event>
    abstract val messagesSent: List<MessageL2ToL1>

    val isAccepted: Boolean
        get() = (
            executionStatus == TransactionExecutionStatus.SUCCEEDED &&
                (finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L1 || finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L2)
            )
}

@Serializable
sealed class ProcessedTransactionReceipt : TransactionReceipt() {
    abstract val blockHash: Felt?
    abstract val blockNumber: Int?

    val isPending: Boolean
        get() = blockHash == null || blockNumber == null
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
    override val events: List<Event>,

    @JsonNames("messages_sent", "l2_to_l1_messages")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("l1_to_l2_consumed_message")
    val messageL1ToL2: MessageL1ToL2? = null,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt? = null,

    @JsonNames("block_number")
    override val blockNumber: Int? = null,

    @JsonNames("status")
    val status: TransactionStatus = TransactionStatus.UNKNOWN,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("revert_error", "revert_reason")
    override val revertReason: String? = null,

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

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt?,

    @JsonNames("block_number")
    override val blockNumber: Int?,

    override val type: TransactionReceiptType,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeployRpcTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt?,

    @JsonNames("block_number")
    override val blockNumber: Int?,

    override val type: TransactionReceiptType,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

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
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("events")
    override val events: List<Event>,

    override val type: TransactionReceiptType = TransactionReceiptType.PENDING,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

) : TransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingRpcDeployTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("events")
    override val events: List<Event>,

    override val type: TransactionReceiptType = TransactionReceiptType.PENDING_DEPLOY,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("contract_address")
    val contractAddress: Felt? = null,
) : TransactionReceipt()
