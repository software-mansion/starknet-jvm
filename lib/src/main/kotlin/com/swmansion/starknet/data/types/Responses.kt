package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.HexToIntDeserializer
import com.swmansion.starknet.data.serializers.NotSyncingResponseSerializer
import com.swmansion.starknet.extensions.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.math.BigInteger
import kotlin.math.roundToInt

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

@Serializable
data class EstimateFeeResponse(
    @SerialName("l1_gas_consumed")
    val l1GasConsumed: Felt,

    @SerialName("l1_gas_price")
    val l1GasPrice: Felt,

    @SerialName("l2_gas_consumed")
    val l2GasConsumed: Felt,

    @SerialName("l2_gas_price")
    val l2GasPrice: Felt,

    @SerialName("l1_data_gas_consumed")
    val l1DataGasConsumed: Felt,

    @SerialName("l1_data_gas_price")
    val l1DataGasPrice: Felt,

    @SerialName("overall_fee")
    val overallFee: Felt,

    // TODO: (#344) Deviation from the spec, make this non-nullable once Pathfinder is updated
    @SerialName("unit")
    val feeUnit: PriceUnit? = null,
) : StarknetResponse {
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
     * Calculates max amount l1 as maxAmountL1 = [overallFee] / [l1GasPrice], unless [l1GasPrice] is 0, then maxAmountL1 is 0.
     * Calculates max amount l2 as maxAmountL2 = [overallFee] / [l2GasPrice], unless [l2GasPrice] is 0, then maxAmountL2 is 0.
     * Calculates max price per unit l1 as maxPricePerUnitL1 = [l1GasPrice].
     * Calculates max price per unit l2 as maxPricePerUnitL2 = [l2GasPrice].
     * Then multiplies maxAmountL1/L2 by round([amountMultiplier] * 100%) and maxPricePerUnitL1/L2 by round([unitPriceMultiplier] * 100%) and performs integer division by 100 on both.
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

        val maxAmountL1 = when (l1GasPrice) {
            Felt.ZERO -> Uint64.ZERO
            else -> (overallFee.value / l1GasPrice.value).applyMultiplier(amountMultiplier).toUint64
        }

        val maxAmountL2 = when (l2GasPrice) {
            Felt.ZERO -> Uint64.ZERO
            else -> (overallFee.value / l2GasPrice.value).applyMultiplier(amountMultiplier).toUint64
        }

        val maxPricePerUnitL1 = l1GasPrice.value.applyMultiplier(unitPriceMultiplier).toUint128
        val maxPricePerUnitL2 = l2GasPrice.value.applyMultiplier(unitPriceMultiplier).toUint128

        return ResourceBoundsMapping(
            l1Gas = ResourceBounds(maxAmount = maxAmountL1, maxPricePerUnit = maxPricePerUnitL1),
            l2Gas = ResourceBounds(maxAmount = maxAmountL2, maxPricePerUnit = maxPricePerUnitL2),
        )
    }

    private fun BigInteger.applyMultiplier(multiplier: Double): BigInteger {
        return (this * (multiplier * 100).roundToInt().toBigInteger()) / BigInteger.valueOf(100)
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
data class GetMessagesStatueResponse(
    @SerialName("transaction_hash")
    val transactionHash: Felt,

    @SerialName("finality_status")
    val finalityStatus: TransactionStatus,

    @SerialName("failure_reason")
    val failureReason: String? = null,
) : StarknetResponse

@Serializable
data class StorageProof(
    @SerialName("classes_proof")
    val classesProof: NodeHashToNodeMapping,

    @SerialName("contracts_proof")
    val contractsProof: NodeHashToNodeMapping,

    @SerialName("contracts_storage_proofs")
    val contractsStorageProofs: List<NodeHashToNodeMapping>,
) : StarknetResponse

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
sealed class StateUpdate : StarknetResponse {
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

// TODO: remove SCREAMING_SNAKE_CASE @JsonNames once devnet is updated
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ResourceBoundsMapping(
    @SerialName("l1_gas")
    @JsonNames("L1_GAS")
    val l1Gas: ResourceBounds,

    @SerialName("l2_gas")
    @JsonNames("L2_GAS")
    val l2Gas: ResourceBounds,
)

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

@Serializable
data class MerkleNode(
    @SerialName("path")
    val path: Int,

    @SerialName("length")
    val length: Int,

    @SerialName("value")
    val value: Felt,

    @SerialName("children_hashes")
    val childrenHashes: ChildrenHashes? = null
)

@Serializable
data class ChildrenHashes(
    @SerialName("left")
    val left: Felt,

    @SerialName("right")
    val right: Felt
)

@Serializable
data class NodeHashToNodeMapping(
    @SerialName("node_hash")
    val nodeHash: Felt,

    @SerialName("node")
    val node: MerkleNode
)
