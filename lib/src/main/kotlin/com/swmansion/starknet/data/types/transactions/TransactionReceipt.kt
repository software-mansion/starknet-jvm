package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MessageL1ToL2
import com.swmansion.starknet.data.types.MessageL2ToL1
import com.swmansion.starknet.data.types.NumAsHex
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

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

    // Not in RPC spec
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

    // Not in RPC spec
    @JsonNames("RECEIVED", "PENDING")
    RECEIVED,

    // Not in RPC spec
    @JsonNames("NOT_RECEIVED", "UNKNOWN")
    NOT_RECEIVED,
}

@Serializable
sealed class TransactionReceipt {
    abstract val hash: Felt
    abstract val type: TransactionType?
    abstract val actualFee: Felt
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
    abstract val isPending: Boolean
}

@Serializable
sealed class ProcessedTransactionReceipt : TransactionReceipt() {
    abstract val blockHash: Felt
    abstract val blockNumber: Int
    override val isPending: Boolean = false
}

@Serializable
sealed class PendingTransactionReceipt : TransactionReceipt() {
    override val isPending: Boolean = true
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
    override val actualFee: Felt,

    @JsonNames("block_hash")
    val blockHash: Felt? = null,

    @JsonNames("block_number")
    val blockNumber: Int? = null,

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

    @JsonNames("type")
    override val type: TransactionType? = null,

    override val isPending: Boolean = blockHash == null || blockNumber == null,
) : TransactionReceipt()

@Serializable
sealed class ProcessedRpcTransactionReceipt : ProcessedTransactionReceipt() {
    abstract val executionResources: ExecutionResources
    abstract override val type: TransactionType
}

@Serializable
sealed class PendingRpcTransactionReceipt : PendingTransactionReceipt() {
    abstract val executionResources: ExecutionResources
    abstract override val type: TransactionType
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedInvokeRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.INVOKE,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,
) : ProcessedRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingInvokeRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.INVOKE,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,
) : PendingRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedDeclareRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    override val type: TransactionType = TransactionType.DECLARE,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,
) : ProcessedRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingDeclareRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.DECLARE,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : PendingRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedDeployAccountRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : ProcessedRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingDeployAccountRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : PendingRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedDeployRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("type")
    override val type: TransactionType,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : ProcessedRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedL1HandlerRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.L1_HANDLER,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("message_hash")
    val messageHash: NumAsHex,
) : ProcessedRpcTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingL1HandlerRpcTransactionReceipt(
    @JsonNames("transaction_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("type")
    override val type: TransactionType = TransactionType.L1_HANDLER,

    @JsonNames("revert_reason", "revert_error")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("message_hash")
    val messageHash: Felt,
) : PendingRpcTransactionReceipt()
