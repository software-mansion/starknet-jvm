package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.HexToIntDeserializer
import com.swmansion.starknet.data.types.transactions.Transaction
import com.swmansion.starknet.data.serializers.TransactionPolymorphicSerializer
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

    abstract val startingBlockNumber: Int

    abstract val currentBlockHash: Felt

    abstract val currentBlockNumber: Int

    abstract val highestBlockHash: Felt

    abstract val highestBlockNumber: Int
}

@Serializable
data class NotSyncingResponse(
    override val status: Boolean,

    override val startingBlockHash: Felt = Felt.ZERO,

    override val startingBlockNumber: Int = 0,

    override val currentBlockHash: Felt = Felt.ZERO,

    override val currentBlockNumber: Int = 0,

    override val highestBlockHash: Felt = Felt.ZERO,

    override val highestBlockNumber: Int = 0,
) : Syncing()

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SyncingResponse(
    override val status: Boolean = true,

    @JsonNames("starting_block_hash")
    override val startingBlockHash: Felt,

    @Serializable(with = HexToIntDeserializer::class)
    @JsonNames("starting_block_num")
    override val startingBlockNumber: Int,

    @JsonNames("current_block_hash")
    override val currentBlockHash: Felt,

    @Serializable(with = HexToIntDeserializer::class)
    @JsonNames("current_block_num")
    override val currentBlockNumber: Int,

    @JsonNames("highest_block_hash")
    override val highestBlockHash: Felt,

    @Serializable(with = HexToIntDeserializer::class)
    @JsonNames("highest_block_num")
    override val highestBlockNumber: Int,
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
    val status: BlockStatus,

    // Block header

    @SerialName("parent_hash")
    override val parentHash: Felt,

    @SerialName("block_hash")
    val blockHash: Felt,

    @SerialName("block_number")
    val blockNumber: Int,

    @SerialName("new_root")
    val newRoot: Felt,

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    // Block body

    @SerialName("transactions")
    override val transactions: List<
        @Serializable(with = TransactionPolymorphicSerializer::class)
        Transaction,
        >,

) : GetBlockWithTransactionsResponse()

@Serializable
data class PendingBlockWithTransactionsResponse(
    // Block body

    @SerialName("transactions")
    override val transactions: List<
        @Serializable(with = TransactionPolymorphicSerializer::class)
        Transaction,
        >,

    // Block info

    @SerialName("timestamp")
    override val timestamp: Int,

    @SerialName("sequencer_address")
    override val sequencerAddress: Felt,

    @SerialName("parent_hash")
    override val parentHash: Felt,
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
    val storageDiffs: List<StorageDiffItem>,

    @SerialName("deprecated_declared_classes")
    val deprecatedDeclaredClasses: List<Felt>,

    @SerialName("declared_classes")
    val declaredClasses: List<DeclaredClassItem>,

    @SerialName("deployed_contracts")
    val deployedContracts: List<DeployedContractItem>,

    @SerialName("replaced_classes")
    val replacedClasses: List<ReplacedClassItem>,

    @SerialName("nonces")
    val nonces: List<NonceItem>,
)

@Serializable
data class DeclaredClassItem(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,
)

@Serializable
data class ReplacedClassItem(
    @SerialName("contract_address")
    val address: Felt,

    @SerialName("class_hash")
    val classHash: Felt,
)

@Serializable
sealed class StateUpdate {
    abstract val oldRoot: Felt
    abstract val stateDiff: StateDiff
}

@Serializable
data class StateUpdateResponse(
    @SerialName("block_hash")
    val blockHash: Felt,

    @SerialName("new_root")
    val newRoot: Felt,

    @SerialName("old_root")
    override val oldRoot: Felt,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,
) : StateUpdate()

@Serializable
data class PendingStateUpdateResponse(
    @SerialName("old_root")
    override val oldRoot: Felt,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,
) : StateUpdate()
