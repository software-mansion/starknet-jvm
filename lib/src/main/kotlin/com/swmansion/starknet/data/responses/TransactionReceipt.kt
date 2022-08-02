package com.swmansion.starknet.data.responses

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.TransactionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
sealed class CommonTransactionReceipt {
    abstract val hash: Felt
    abstract val actualFee: Felt
}

@Serializable
sealed class TransactionReceipt : CommonTransactionReceipt() {
    abstract override val hash: Felt
    abstract override val actualFee: Felt
    abstract val status: TransactionStatus
    abstract val rejectionReason: String?
    abstract val blockHash: Felt
    abstract val blockNumber: Int
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GatewayTransactionReceipt(
    @JsonNames("events")
    val events: List<Event>,

    @JsonNames("l2_to_l1_messages")
    val messageToL1: List<MessageToL1>,

    @JsonNames("l1_to_l2_consumed_message")
    val messageToL2: MessageToL2? = null,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("transaction_failure_reason")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,
) : TransactionReceipt()

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

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("status_data")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,
) : TransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("status_data")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,
) : TransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeployTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("status_data")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int,
) : TransactionReceipt()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PendingTransactionReceipt(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt,
) : CommonTransactionReceipt()

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
) : CommonTransactionReceipt()
