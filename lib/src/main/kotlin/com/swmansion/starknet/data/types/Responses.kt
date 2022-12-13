package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.transactions.Transaction
import com.swmansion.starknet.data.types.transactions.TransactionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class CallContractResponse(
    val result: List<Felt>,
)

@Serializable
data class InvokeFunctionResponse(
    @SerialName("transaction_hash") val transactionHash: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("class_hash")
    val classHash: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeployAccountResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("address", "contract_address")
    val address: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EstimateFeeResponse(
    @JsonNames("gas_consumed", "gas_usage")
    val gasConsumed: Felt,

    @JsonNames("gas_price")
    val gasPrice: Felt,

    @JsonNames("overall_fee")
    val overallFee: Felt,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GetBlockHashAndNumberResponse(
    @JsonNames("block_hash")
    val blockHash: Felt,

    @JsonNames("block_number")
    val blockNumber: Int,
)

@Serializable
sealed class Syncing {
    abstract val status: Boolean

    abstract val startingBlockHash: Felt

    abstract val startingBlockNumber: Felt

    abstract val currentBlockHash: Felt

    abstract val currentBlockNumber: Felt

    abstract val highestBlockHash: Felt

    abstract val highestBlockNumber: Felt
}

@Serializable
data class NotSyncingResponse(
    override val status: Boolean,

    override val startingBlockHash: Felt = Felt.ZERO,

    override val startingBlockNumber: Felt = Felt.ZERO,

    override val currentBlockHash: Felt = Felt.ZERO,

    override val currentBlockNumber: Felt = Felt.ZERO,

    override val highestBlockHash: Felt = Felt.ZERO,

    override val highestBlockNumber: Felt = Felt.ZERO,
) : Syncing()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SyncingResponse(
    override val status: Boolean = true,

    @JsonNames("starting_block_hash")
    override val startingBlockHash: Felt,

    @JsonNames("starting_block_num")
    override val startingBlockNumber: Felt,

    @JsonNames("current_block_hash")
    override val currentBlockHash: Felt,

    @JsonNames("current_block_num")
    override val currentBlockNumber: Felt,

    @JsonNames("highest_block_hash")
    override val highestBlockHash: Felt,

    @JsonNames("highest_block_num")
    override val highestBlockNumber: Felt,
) : Syncing()

@Serializable
sealed class GetBlockWithTransactionsResponse {
    abstract val transactions: List<Transaction>
    abstract val timestamp: Int
    abstract val sequencerAddress: Felt
    abstract val parentHash: Felt
}

@Serializable
data class BlockWithTransactionsResponse(
    @SerialName("status")
    val status: TransactionStatus,

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("block_hash")
    val blockHash: Felt,

    @SerialName("block_number")
    val blockNumber: Int,

    @SerialName("new_root")
    val newRoot: Felt,

    @SerialName("transactions")
    override val transactions: List<Transaction>,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,
) : GetBlockWithTransactionsResponse()

@Serializable
data class PendingBlockWithTransactionsResponse(
    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("transactions")
    override val transactions: List<Transaction>,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,
) : GetBlockWithTransactionsResponse()

@Serializable
data class StorageEntries(
    @SerialName("key")
    val key: Felt,

    @SerialName("value")
    val value: Felt,
)

@Serializable
data class StorageDiffItem(
    @SerialName("address")
    val address: Felt,

    @SerialName("storage_entries")
    val storageEntries: List<StorageEntries>,
)

@Serializable
data class DeployedContractItem(
    @SerialName("address")
    val address: Felt,

    @SerialName("class_hash")
    val classHash: Felt,
)

@Serializable
data class NonceItem(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("nonce")
    val nonce: Felt,
)

@Serializable
data class StateDiff(
    @SerialName("storage_diffs")
    val storageDiffs: List<StorageDiffItem?>,

    @SerialName("declared_contract_hashes")
    val declaredContractHashes: List<Felt?>,

    @SerialName("deployed_contracts")
    val deployedContracts: List<DeployedContractItem?>,

    @SerialName("nonces")
    val nonces: List<NonceItem?>,
)

@Serializable
data class StateUpdateResponse(
    @SerialName("block_hash")
    val blockHash: Felt,

    @SerialName("new_root")
    val newRoot: Felt,

    @SerialName("old_root")
    val oldRoot: Felt,

    @SerialName("state_diff")
    val stateDiff: StateDiff,
)
