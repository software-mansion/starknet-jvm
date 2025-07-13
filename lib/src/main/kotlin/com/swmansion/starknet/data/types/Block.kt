package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.BlockIdSerializer
import com.swmansion.starknet.data.serializers.TransactionReceiptPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BlockTag(val tag: String) {
    LATEST("latest"), PRE_CONFIRMED("pre_confirmed")
}

@Serializable
enum class BlockStatus {
    PRE_CONFIRMED,
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
    val blockNumber: Int
    val l1GasPrice: ResourcePrice
    val l2GasPrice: ResourcePrice
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
    val newRoot: Felt
}

/**
 * Represents a pre-confirmed block.
 *
 * Corresponds to the `PRE_CONFIRMED_BLOCK_HEADER` schema defined in the JSON-RPC spec.
 */
sealed interface PreConfirmedBlock : Block

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
        @Serializable(with = TransactionSerializer::class)
        Transaction,
        >,

    @SerialName("parent_hash")
    val parentHash: Felt,

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

    @SerialName("l2_gas_price")
    override val l2GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactions(), ProcessedBlock

@Serializable
data class PreConfirmedBlockWithTransactions(
    @SerialName("transactions")
    override val transactions: List<
        @Serializable(with = TransactionSerializer::class)
        Transaction,
        >,

    @SerialName("block_number")
    override val blockNumber: Int,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l2_gas_price")
    override val l2GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactions(), PreConfirmedBlock

@Serializable
data class TransactionWithReceipt(
    @Serializable(with = TransactionSerializer::class)
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
    val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l2_gas_price")
    override val l2GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithReceipts(), ProcessedBlock

@Serializable
data class PreConfirmedBlockWithReceipts(
    @SerialName("transactions")
    override val transactionsWithReceipts: List<TransactionWithReceipt>,

    @SerialName("block_number")
    override val blockNumber: Int,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l2_gas_price")
    override val l2GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithReceipts(), PreConfirmedBlock

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
    val parentHash: Felt,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l2_gas_price")
    override val l2GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactionHashes(), ProcessedBlock

@Serializable
data class PreConfirmedBlockWithTransactionHashes(
    @SerialName("transactions")
    override val transactionHashes: List<Felt>,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("block_number")
    override val blockNumber: Int,

    @SerialName("l1_gas_price")
    override val l1GasPrice: ResourcePrice,

    @SerialName("l2_gas_price")
    override val l2GasPrice: ResourcePrice,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: ResourcePrice,

    @SerialName("l1_da_mode")
    override val l1DataAvailabilityMode: L1DAMode,

    @SerialName("starknet_version")
    override val starknetVersion: String,
) : BlockWithTransactionHashes(), PreConfirmedBlock
