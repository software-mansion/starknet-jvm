package com.swmansion.starknet.data.types

import MerkleNodePolymorphicSerializer
import com.swmansion.starknet.data.serializers.HexToIntDeserializer
import com.swmansion.starknet.data.serializers.NotSyncingResponseSerializer
import com.swmansion.starknet.extensions.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.math.BigInteger
import kotlin.math.roundToInt

typealias NodeHashToNodeMapping = List<NodeHashToNodeMappingItem>

@Serializable
data class CallContractResponse(
    val result: List<Felt>,
)

@Serializable
data class InvokeFunctionResponse(
    @SerialName("transaction_hash") val transactionHash: Felt,
) : StarknetResponse

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeclareResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("class_hash")
    val classHash: Felt,
) : StarknetResponse

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DeployAccountResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    // TODO: (#344) deviation from the spec, make this non-nullable once Juno is updated
    @JsonNames("address", "contract_address")
    val address: Felt? = null,
) : StarknetResponse

sealed class EstimateFeeCommon : StarknetResponse {
    abstract val l1GasConsumed: Uint64
    abstract val l1GasPrice: Uint128
    abstract val l2GasConsumed: Uint64
    abstract val l2GasPrice: Uint128
    abstract val l1DataGasConsumed: Uint64
    abstract val l1DataGasPrice: Uint128
    abstract val overallFee: Uint128
}

@Serializable
data class EstimateFeeResponse(
    @SerialName("l1_gas_consumed")
    override val l1GasConsumed: Uint64,

    @SerialName("l1_gas_price")
    override val l1GasPrice: Uint128,

    @SerialName("l2_gas_consumed")
    override val l2GasConsumed: Uint64,

    @SerialName("l2_gas_price")
    override val l2GasPrice: Uint128,

    @SerialName("l1_data_gas_consumed")
    override val l1DataGasConsumed: Uint64,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: Uint128,

    @SerialName("overall_fee")
    override val overallFee: Uint128,

    @SerialName("unit")
    val feeUnit: PriceUnit,
) : StarknetResponse, EstimateFeeCommon() {
    init {
        require(feeUnit == PriceUnit.FRI) {
            "`feeUnit` for `EstimateFeeResponse` can only be FRI"
        }
    }

    /**
     * Convert estimated fee to max fee with applied multiplier.
     *
     * Multiplies [overallFee] by round([multiplier] * 100%) and performs integer division by 100.
     *
     * @param multiplier Multiplier for max fee, defaults to 1.5.
     */
    @JvmOverloads
    fun toMaxFee(multiplier: Double = 1.5): Felt {
        require(multiplier >= 0)

        return overallFee.value.applyMultiplier(multiplier).toFelt
    }

    /**
     * Convert estimated fee to resource bounds with applied multipliers.
     *
     * Calculates max amount of l1 gas as [l1GasConsumed] * [amountMultiplier] and max price per unit as [l1GasPrice] * [unitPriceMultiplier].
     * Calculates max amount of l2 gas as [l2GasConsumed] * [amountMultiplier] and max price per unit as [l2GasPrice] * [unitPriceMultiplier].
     * Calculates max amount of l1 data gas as [l1DataGasConsumed] * [amountMultiplier] and max price per unit as [l1DataGasPrice] * [unitPriceMultiplier].
     *
     * @param amountMultiplier Multiplier for max amount, defaults to 1.5.
     * @param unitPriceMultiplier Multiplier for max price per unit, defaults to 1.5.
     *
     * @return Resource bounds with applied multipliers.
     */
    @JvmOverloads
    fun toResourceBounds(
        amountMultiplier: Double = 1.5,
        unitPriceMultiplier: Double = 1.5,
    ): ResourceBoundsMapping {
        require(amountMultiplier >= 0)
        require(unitPriceMultiplier >= 0)

        return ResourceBoundsMapping(
            l1Gas = ResourceBounds(maxAmount = l1GasConsumed.value.applyMultiplier(amountMultiplier).toUint64, maxPricePerUnit = l1GasPrice.value.applyMultiplier(unitPriceMultiplier).toUint128),
            l1DataGas = ResourceBounds(maxAmount = l1DataGasConsumed.value.applyMultiplier(amountMultiplier).toUint64, maxPricePerUnit = l1DataGasPrice.value.applyMultiplier(unitPriceMultiplier).toUint128),
            l2Gas = ResourceBounds(maxAmount = l2GasConsumed.value.applyMultiplier(amountMultiplier).toUint64, maxPricePerUnit = l2GasPrice.value.applyMultiplier(unitPriceMultiplier).toUint128),
        )
    }

    private fun BigInteger.applyMultiplier(multiplier: Double): BigInteger {
        return (this * (multiplier * 100).roundToInt().toBigInteger()) / BigInteger.valueOf(100)
    }
}

@Serializable
data class EstimateMessageFeeResponse(
    @SerialName("l1_gas_consumed")
    override val l1GasConsumed: Uint64,

    @SerialName("l1_gas_price")
    override val l1GasPrice: Uint128,

    @SerialName("l2_gas_consumed")
    override val l2GasConsumed: Uint64,

    @SerialName("l2_gas_price")
    override val l2GasPrice: Uint128,

    @SerialName("l1_data_gas_consumed")
    override val l1DataGasConsumed: Uint64,

    @SerialName("l1_data_gas_price")
    override val l1DataGasPrice: Uint128,

    @SerialName("overall_fee")
    override val overallFee: Uint128,

    @SerialName("unit")
    val feeUnit: PriceUnit,
) : StarknetResponse, EstimateFeeCommon() {
    init {
        require(feeUnit == PriceUnit.WEI) {
            "`feeUnit` for `EstimateMessageFeeResponse` can only be WEI"
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GetBlockHashAndNumberResponse(
    @JsonNames("block_hash")
    val blockHash: Felt,

    @JsonNames("block_number")
    val blockNumber: Int,
) : StarknetResponse

@Serializable
data class GetTransactionStatusResponse(
    @SerialName("finality_status")
    val finalityStatus: TransactionStatus,

    @SerialName("execution_status")
    val executionStatus: TransactionExecutionStatus? = null,

    @SerialName("failure_reason")
    val failureReason: String? = null,
) : StarknetResponse

@Serializable
data class MessageStatus(
    @SerialName("transaction_hash")
    val transactionHash: Felt,

    @SerialName("finality_status")
    val finalityStatus: TransactionFinalityStatus,

    @SerialName("failure_reason")
    val failureReason: String? = null,
)

@Serializable
data class StorageProof(
    @SerialName("classes_proof")
    val classesProof: NodeHashToNodeMapping,

    @SerialName("contracts_proof")
    val contractsProof: ContractsProof,

    @SerialName("contracts_storage_proofs")
    val contractsStorageProofs: List<NodeHashToNodeMapping>,

    @SerialName("global_roots")
    val globalRoots: GlobalRoots,
) : StarknetResponse {
    @Serializable
    data class GlobalRoots(
        @SerialName("contracts_tree_root")
        val contractsTreeRoot: Felt,

        @SerialName("classes_tree_root")
        val classesTreeRoot: Felt,

        @SerialName("block_hash")
        val blockHash: Felt,
    )
}

@Serializable
data class ContractsProof(
    @SerialName("nodes")
    val nodes: NodeHashToNodeMapping,

    @SerialName("contract_leaves_data")
    val contractLeavesData: List<ContractLeafData>,
)

@Serializable
data class ContractLeafData(
    @SerialName("nonce")
    val nonce: Felt,

    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("storage_root")
    val storageRoot: Felt? = null,
)

@Serializable
data class NodeHashToNodeMappingItem(
    @SerialName("node_hash")
    val nodeHash: Felt,

    @SerialName("node")
    val node: MerkleNode,
) {
    @Serializable(with = MerkleNodePolymorphicSerializer::class)
    sealed interface MerkleNode

    @Serializable
    data class BinaryNode(
        @SerialName("left")
        val left: Felt,

        @SerialName("right")
        val right: Felt,
    ) : MerkleNode

    @Serializable
    data class EdgeNode(
        @SerialName("path")
        val path: NumAsHex,

        @SerialName("length")
        val length: UInt,

        @SerialName("child")
        val value: Felt,
    ) : MerkleNode
}

@Serializable
sealed class Syncing : StarknetResponse {
    abstract val status: Boolean

    abstract val startingBlockHash: Felt

    abstract val startingBlockNumber: Int

    abstract val currentBlockHash: Felt

    abstract val currentBlockNumber: Int

    abstract val highestBlockHash: Felt

    abstract val highestBlockNumber: Int
}

@Suppress("DataClassPrivateConstructor")
@Serializable(with = NotSyncingResponseSerializer::class)
data class NotSyncingResponse private constructor(
    override val status: Boolean,

    override val startingBlockHash: Felt,

    override val startingBlockNumber: Int,

    override val currentBlockHash: Felt,

    override val currentBlockNumber: Int,

    override val highestBlockHash: Felt,

    override val highestBlockNumber: Int,
) : Syncing() {
    constructor(status: Boolean) : this(
        status = status,
        startingBlockHash = Felt.ZERO,
        startingBlockNumber = 0,
        currentBlockHash = Felt.ZERO,
        currentBlockNumber = 0,
        highestBlockHash = Felt.ZERO,
        highestBlockNumber = 0,
    )
}

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

    @SerialName("migrated_compiled_classes")
    val migratedCompiledClasses: List<MigratedClassItem>? = null,

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
data class MigratedClassItem(
    @SerialName("class_hash")
    val classHash: Felt,

    @SerialName("compiled_class_hash")
    val compiledClassHash: Felt,
)

@Serializable
sealed class StateUpdate : StarknetResponse {
    abstract val oldRoot: Felt?
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
data class PreConfirmedStateUpdateResponse(
    @SerialName("old_root")
    override val oldRoot: Felt?,

    @SerialName("state_diff")
    override val stateDiff: StateDiff,
) : StateUpdate()

@Serializable
data class ResourceBoundsMapping(
    @SerialName("l1_gas")
    val l1Gas: ResourceBounds,

    @SerialName("l1_data_gas")
    val l1DataGas: ResourceBounds,

    @SerialName("l2_gas")
    val l2Gas: ResourceBounds,
) {
    companion object {
        @field:JvmField
        val ZERO = ResourceBoundsMapping(ResourceBounds.ZERO, ResourceBounds.ZERO, ResourceBounds.ZERO)
    }
}

@Serializable
data class ResourceBounds(
    @SerialName("max_amount")
    val maxAmount: Uint64,

    @SerialName("max_price_per_unit")
    val maxPricePerUnit: Uint128,
) {
    companion object {
        @field:JvmField
        val ZERO = ResourceBounds(Uint64.ZERO, Uint128.ZERO)
    }

    fun toMaxFee(): Felt {
        return maxAmount.value.multiply(maxPricePerUnit.value).toFelt
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResourcePrice(
    // TODO: (#344) This is a deviation from the spec, make this non-nullable once Juno is updated
    @SerialName("price_in_wei")
    val priceInWei: Felt? = null,

    @SerialName("price_in_fri")
    @JsonNames("price_in_strk") // TODO: (#344) RPC 0.5.0 legacy name, remove once Pathfinder is updated
    val priceInFri: Felt,
)

@Serializable
data class FeePayment(
    @SerialName("amount")
    val amount: Felt,

    @SerialName("unit")
    val unit: PriceUnit,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class PriceUnit {
    @SerialName("WEI")
    WEI,

    @SerialName("FRI")
    @JsonNames("STRK") // TODO: (#344) RPC 0.5.0 legacy name, remove once Pathfinder is updated
    FRI,
}

@Serializable
enum class L1DAMode {
    @SerialName("BLOB")
    BLOB,

    @SerialName("CALLDATA")
    CALLDATA,
}
