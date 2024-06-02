package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.BlockIdSerializer
import com.swmansion.starknet.data.serializers.TransactionPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionReceiptPolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BlockTag(val tag: String) {
    LATEST("latest"), PENDING("pending");

    companion object {
        fun fromValue(value: String): BlockTag {
            return BlockTag.entries.firstOrNull() { it.tag == value } ?: throw IllegalArgumentException("Unknown block tag: $value")
        }
    }
}

@Serializable
enum class BlockStatus {
    PENDING,
    ACCEPTED_ON_L1,
    ACCEPTED_ON_L2,
    REJECTED,
}

@Serializable(with = BlockIdSerializer::class)
sealed class BlockId {
    data class Hash(
        val blockHash: Felt,
    ) : BlockId() {
        override fun toString(): String {
            return blockHash.hexString()
        }
    }

    data class Number(
        val blockNumber: Int,
    ) : BlockId()

    data class Tag(
        val blockTag: BlockTag,
    ) : BlockId() {
        override fun toString(): String {
            return blockTag.tag
        }
    }
}
sealed interface Block : StarknetResponse {
    val timestamp: Int
    val sequencerAddress: Felt
    val parentHash: Felt
    val l1GasPrice: ResourcePrice
    val l1DataGasPrice: ResourcePrice
    val l1DataAvailabilityMode: L1DAMode
    val starknetVersion: String
}

/**
 * Represents a processed block.
 *
 * Corresponds to the `BLOCK_HEADER` schema defined in the JSON-RPC spec.
 */
sealed interface ProcessedBlock : Block {
    val status: BlockStatus
    val blockHash: Felt
    val blockNumber: Int
    val newRoot: Felt
}

/**
 * Represents a pending block.
 *
 * Corresponds to the `PENDING_BLOCK_HEADER` schema defined in the JSON-RPC spec.
 */
sealed interface PendingBlock : Block

@Serializable
sealed class BlockWithTransactions : Block {
    abstract val transactions: List<Transaction>
}

@Serializable
data class ProcessedBlockWithTransactions(
    @SerialName("status")
    override val status: BlockStatus,

    @SerialName("transactions")
    override val transactions: List<
        @Serializable(with = TransactionPolymorphicSerializer::class)
        Transaction,
        >,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("block_hash")
    override val blockHash: Felt,

    @SerialName("block_number")
    override val blockNumber: Int,

    @SerialName("new_root")
    override val newRoot: Felt,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactions(), ProcessedBlock

@Serializable
data class PendingBlockWithTransactions(
    @SerialName("transactions")
    override val transactions: List<
        @Serializable(with = TransactionPolymorphicSerializer::class)
        Transaction,
        >,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactions(), PendingBlock

@Serializable
data class TransactionWithReceipt(
    @Serializable(with = TransactionPolymorphicSerializer::class)
    @SerialName("transaction")
    val transaction: Transaction,

    @Serializable(with = TransactionReceiptPolymorphicSerializer::class)
    @SerialName("receipt")
    val receipt: TransactionReceipt,
)

@Serializable
sealed class BlockWithReceipts : Block {
    abstract val transactionsWithReceipts: List<TransactionWithReceipt>
}

@Serializable
data class ProcessedBlockWithReceipts(
    @SerialName("status")
    override val status: BlockStatus,

    @SerialName("transactions")
    override val transactionsWithReceipts: List<TransactionWithReceipt>,

    @SerialName("block_hash")
    override val blockHash: Felt,

    @SerialName("block_number")
    override val blockNumber: Int,

    @SerialName("new_root")
    override val newRoot: Felt,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithReceipts(), ProcessedBlock

@Serializable
data class PendingBlockWithReceipts(
    @SerialName("transactions")
    override val transactionsWithReceipts: List<TransactionWithReceipt>,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithReceipts(), PendingBlock

@Serializable
sealed class BlockWithTransactionHashes : Block {
    abstract val transactionHashes: List<Felt>
}

@Serializable
data class ProcessedBlockWithTransactionHashes(
    @SerialName("status")
    override val status: BlockStatus,

    @SerialName("transactions")
    override val transactionHashes: List<Felt>,

    @SerialName("block_hash")
    override val blockHash: Felt,

    @SerialName("block_number")
    override val blockNumber: Int,

    @SerialName("new_root")
    override val newRoot: Felt,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactionHashes(), ProcessedBlock

@Serializable
data class PendingBlockWithTransactionHashes(
    @SerialName("transactions")
    override val transactionHashes: List<Felt>,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactionHashes(), PendingBlock
