package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
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

@Serializable
enum class TransactionExecutionStatus {
    @SerialName("SUCCEEDED")
    SUCCEEDED,

    @SerialName("REVERTED")
    REVERTED,

    // Not in RPC spec
    @SerialName("REJECTED")
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
    abstract val actualFee: FeePayment
    abstract val executionStatus: TransactionExecutionStatus
    abstract val finalityStatus: TransactionFinalityStatus
    abstract val revertReason: String?
    abstract val events: List<Event>
    abstract val messagesSent: List<MessageL2ToL1>
    abstract val executionResources: ExecutionResources
    abstract val blockHash: Felt?
    abstract val blockNumber: Int?

    val isAccepted: Boolean
        get() = (
            executionStatus == TransactionExecutionStatus.SUCCEEDED &&
                (finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L1 || finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L2)
            )

    val isPending: Boolean
        get() = !hasBlockInfo && finalityStatus == TransactionFinalityStatus.ACCEPTED_ON_L2

    /**
     * Checks if the receipt contains block information.
     *
     * This method verifies whether the receipt conforms to the `TXN_RECEIPT_WITH_BLOCK_INFO` schema as defined in the JSON-RPC spec.
     *
     * @return `true` if both [blockHash] and [blockNumber] are not null; `false` otherwise.
     */
    val hasBlockInfo: Boolean
        get() = listOf(blockHash, blockNumber).all { it != null }
}

@Serializable
data class InvokeTransactionReceipt(
    @SerialName("transaction_hash")
    override val hash: Felt,

    @SerialName("actual_fee")
    override val actualFee: FeePayment,

    @SerialName("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @SerialName("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @SerialName("block_hash")
    override val blockHash: Felt? = null,

    @SerialName("block_number")
    override val blockNumber: Int? = null,

    @SerialName("type")
    override val type: TransactionType = TransactionType.INVOKE,

    @SerialName("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @SerialName("revert_reason")
    override val revertReason: String? = null,

    @SerialName("events")
    override val events: List<Event>,

    @SerialName("execution_resources")
    override val executionResources: ExecutionResources,
) : TransactionReceipt()

@Serializable
data class DeclareTransactionReceipt(
    @SerialName("transaction_hash")
    override val hash: Felt,

    @SerialName("actual_fee")
    override val actualFee: FeePayment,

    @SerialName("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @SerialName("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @SerialName("block_hash")
    override val blockHash: Felt? = null,

    @SerialName("block_number")
    override val blockNumber: Int? = null,

    override val type: TransactionType = TransactionType.DECLARE,

    @SerialName("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @SerialName("revert_reason")
    override val revertReason: String? = null,

    @SerialName("events")
    override val events: List<Event>,

    @SerialName("execution_resources")
    override val executionResources: ExecutionResources,
) : TransactionReceipt()

@Serializable
data class DeployAccountTransactionReceipt(
    @SerialName("transaction_hash")
    override val hash: Felt,

    @SerialName("actual_fee")
    override val actualFee: FeePayment,

    @SerialName("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @SerialName("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @SerialName("block_hash")
    override val blockHash: Felt? = null,

    @SerialName("block_number")
    override val blockNumber: Int? = null,

    @SerialName("type")
    override val type: TransactionType = TransactionType.DEPLOY_ACCOUNT,

    @SerialName("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @SerialName("revert_reason")
    override val revertReason: String? = null,

    @SerialName("events")
    override val events: List<Event>,

    @SerialName("execution_resources")
    override val executionResources: ExecutionResources,

    @SerialName("contract_address")
    val contractAddress: Felt,
) : TransactionReceipt()

@Serializable
data class DeployTransactionReceipt(
    @SerialName("transaction_hash")
    override val hash: Felt,

    @SerialName("actual_fee")
    override val actualFee: FeePayment,

    @SerialName("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @SerialName("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @SerialName("block_hash")
    override val blockHash: Felt? = null,

    @SerialName("block_number")
    override val blockNumber: Int? = null,

    @SerialName("type")
    override val type: TransactionType,

    @SerialName("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @SerialName("revert_reason")
    override val revertReason: String? = null,

    @SerialName("events")
    override val events: List<Event>,

    @SerialName("execution_resources")
    override val executionResources: ExecutionResources,

    @SerialName("contract_address")
    val contractAddress: Felt,
) : TransactionReceipt()

@Serializable
data class L1HandlerTransactionReceipt(
    @SerialName("transaction_hash")
    override val hash: Felt,

    @SerialName("actual_fee")
    override val actualFee: FeePayment,

    @SerialName("execution_status")
    override val executionStatus: TransactionExecutionStatus,

    @SerialName("finality_status")
    override val finalityStatus: TransactionFinalityStatus,

    @SerialName("block_hash")
    override val blockHash: Felt? = null,

    @SerialName("block_number")
    override val blockNumber: Int? = null,

    @SerialName("type")
    override val type: TransactionType = TransactionType.L1_HANDLER,

    @SerialName("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @SerialName("revert_reason")
    override val revertReason: String? = null,

    @SerialName("events")
    override val events: List<Event>,

    @SerialName("execution_resources")
    override val executionResources: ExecutionResources,

    @SerialName("message_hash")
    val messageHash: NumAsHex,
) : TransactionReceipt()
