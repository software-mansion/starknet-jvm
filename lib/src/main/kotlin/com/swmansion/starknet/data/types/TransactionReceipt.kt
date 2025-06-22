package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class TransactionStatus {
    @JsonNames("RECEIVED")
    RECEIVED,

    @JsonNames("CANDIDATE")
    CANDIDATE,

    @JsonNames("PRE_CONFIRMED")
    PRE_CONFIRMED,

    @JsonNames("ACCEPTED_ON_L1")
    ACCEPTED_ON_L1,

    @JsonNames("ACCEPTED_ON_L2")
    ACCEPTED_ON_L2,
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
sealed class TransactionReceipt : StarknetResponse {
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
     * @return `true` if both [blockHash] and [blockNumber] are not `null`; `false` otherwise.
     */
    val hasBlockInfo: Boolean
        get() = listOf(blockHash, blockNumber).all { it != null }
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class InvokeTransactionReceipt private constructor(
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
) : TransactionReceipt() {
    @JvmOverloads constructor(
        hash: Felt,
        actualFee: FeePayment,
        executionStatus: TransactionExecutionStatus,
        finalityStatus: TransactionFinalityStatus,
        blockHash: Felt? = null,
        blockNumber: Int? = null,
        messagesSent: List<MessageL2ToL1>,
        revertReason: String? = null,
        events: List<Event>,
        executionResources: ExecutionResources,
    ) : this(
        hash = hash,
        actualFee = actualFee,
        executionStatus = executionStatus,
        finalityStatus = finalityStatus,
        blockHash = blockHash,
        blockNumber = blockNumber,
        type = TransactionType.INVOKE,
        messagesSent = messagesSent,
        revertReason = revertReason,
        events = events,
        executionResources = executionResources,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class DeclareTransactionReceipt private constructor(
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

    override val type: TransactionType,

    @SerialName("messages_sent")
    override val messagesSent: List<MessageL2ToL1>,

    @SerialName("revert_reason")
    override val revertReason: String? = null,

    @SerialName("events")
    override val events: List<Event>,

    @SerialName("execution_resources")
    override val executionResources: ExecutionResources,
) : TransactionReceipt() {
    @JvmOverloads constructor(
        hash: Felt,
        actualFee: FeePayment,
        executionStatus: TransactionExecutionStatus,
        finalityStatus: TransactionFinalityStatus,
        blockHash: Felt? = null,
        blockNumber: Int? = null,
        messagesSent: List<MessageL2ToL1>,
        revertReason: String? = null,
        events: List<Event>,
        executionResources: ExecutionResources,
    ) : this(
        hash = hash,
        actualFee = actualFee,
        executionStatus = executionStatus,
        finalityStatus = finalityStatus,
        blockHash = blockHash,
        blockNumber = blockNumber,
        type = TransactionType.DECLARE,
        messagesSent = messagesSent,
        revertReason = revertReason,
        events = events,
        executionResources = executionResources,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class DeployAccountTransactionReceipt private constructor(
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
) : TransactionReceipt() {
    @JvmOverloads constructor(
        hash: Felt,
        actualFee: FeePayment,
        executionStatus: TransactionExecutionStatus,
        finalityStatus: TransactionFinalityStatus,
        blockHash: Felt? = null,
        blockNumber: Int? = null,
        messagesSent: List<MessageL2ToL1>,
        revertReason: String? = null,
        events: List<Event>,
        executionResources: ExecutionResources,
        contractAddress: Felt,
    ) : this(
        hash = hash,
        actualFee = actualFee,
        executionStatus = executionStatus,
        finalityStatus = finalityStatus,
        blockHash = blockHash,
        blockNumber = blockNumber,
        type = TransactionType.DEPLOY_ACCOUNT,
        messagesSent = messagesSent,
        revertReason = revertReason,
        events = events,
        executionResources = executionResources,
        contractAddress = contractAddress,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class DeployTransactionReceipt private constructor(
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
) : TransactionReceipt() {
    @JvmOverloads constructor(
        hash: Felt,
        actualFee: FeePayment,
        executionStatus: TransactionExecutionStatus,
        finalityStatus: TransactionFinalityStatus,
        blockHash: Felt? = null,
        blockNumber: Int? = null,
        messagesSent: List<MessageL2ToL1>,
        revertReason: String? = null,
        events: List<Event>,
        executionResources: ExecutionResources,
        contractAddress: Felt,
    ) : this(
        hash = hash,
        actualFee = actualFee,
        executionStatus = executionStatus,
        finalityStatus = finalityStatus,
        blockHash = blockHash,
        blockNumber = blockNumber,
        type = TransactionType.DEPLOY,
        messagesSent = messagesSent,
        revertReason = revertReason,
        events = events,
        executionResources = executionResources,
        contractAddress = contractAddress,
    )
}

@Suppress("DataClassPrivateConstructor")
@Serializable
data class L1HandlerTransactionReceipt private constructor(
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

    @SerialName("message_hash")
    val messageHash: NumAsHex,
) : TransactionReceipt() {
    @JvmOverloads
    constructor(
        hash: Felt,
        actualFee: FeePayment,
        executionStatus: TransactionExecutionStatus,
        finalityStatus: TransactionFinalityStatus,
        blockHash: Felt? = null,
        blockNumber: Int? = null,
        messagesSent: List<MessageL2ToL1>,
        revertReason: String? = null,
        events: List<Event>,
        executionResources: ExecutionResources,
        messageHash: NumAsHex,
    ) : this(
        hash = hash,
        actualFee = actualFee,
        executionStatus = executionStatus,
        finalityStatus = finalityStatus,
        blockHash = blockHash,
        blockNumber = blockNumber,
        type = TransactionType.L1_HANDLER,
        messagesSent = messagesSent,
        revertReason = revertReason,
        events = events,
        executionResources = executionResources,
        messageHash = messageHash,
    )
}
