package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.Event
import com.swmansion.starknet.data.types.Felt
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
    abstract val type: TransactionType
    abstract val actualFee: Felt?
    abstract val executionStatus: TransactionExecutionStatus
    abstract val finalityStatus: TransactionFinalityStatus
    abstract val revertReason: String?
    abstract val events: List<Event>
    abstract val messagesSent: List<MessageL2ToL1>
    abstract val executionResources: ExecutionResources

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
data class ProcessedInvokeTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingInvokeTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,
) : PendingTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedDeclareTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingDeclareTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : PendingTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedDeployAccountTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingDeployAccountTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : PendingTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedDeployTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("contract_address")
    val contractAddress: Felt,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ProcessedL1HandlerTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("events")
    override val events: List<Event>,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("message_hash")
    val messageHash: NumAsHex,
) : ProcessedTransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingL1HandlerTransactionReceipt(
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

    @JsonNames("revert_reason")
    override val revertReason: String? = null,

    @JsonNames("finality_status")
    override val finalityStatus: TransactionFinalityStatus = TransactionFinalityStatus.ACCEPTED_ON_L2,

    @JsonNames("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @JsonNames("execution_resources")
    override val executionResources: ExecutionResources,

    @JsonNames("message_hash")
    val messageHash: Felt,
) : PendingTransactionReceipt()
