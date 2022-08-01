package com.swmansion.starknet.data.responses

import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.TransactionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import starknet.data.Event
import starknet.data.types.Felt
import starknet.data.types.TransactionStatus

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

@Serializable
// OptIn needed because @JsonNames is part of the experimental serialization api
data class InvokeTransactionReceipt @OptIn(ExperimentalSerializationApi::class) constructor(
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

    @JsonNames("statusData")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int
) : TransactionReceipt()

@Serializable
data class DeclareTransactionReceipt @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("statusData")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int
) : TransactionReceipt()

@Serializable
data class DeployTransactionReceipt @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("status")
    override val status: TransactionStatus,

    @JsonNames("actual_fee")
    override val actualFee: Felt,

    @JsonNames("statusData")
    override val rejectionReason: String? = null,

    @JsonNames("block_hash")
    override val blockHash: Felt,

    @JsonNames("block_number")
    override val blockNumber: Int
) : TransactionReceipt()

@Serializable
data class PendingTransactionReceipt @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt
) : CommonTransactionReceipt()

@Serializable
data class PendingInvokeTransactionReceipt @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("messages_sent")
    val messagesSent: List<MessageToL1>,

    @JsonNames("l1_origin_message")
    val l1OriginMessage: MessageToL2? = null,

    @JsonNames("events")
    val events: List<Event>,

    @JsonNames("transaction_hash", "txn_hash")
    override val hash: Felt,

    @JsonNames("actual_fee")
    override val actualFee: Felt
) : CommonTransactionReceipt()
