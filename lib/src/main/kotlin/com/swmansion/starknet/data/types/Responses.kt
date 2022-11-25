package com.swmansion.starknet.data.types

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
// OptIn needed because @JsonNames is part of the experimental serialization api
data class DeployResponse(
    @JsonNames("transaction_hash")
    val transactionHash: Felt,

    @JsonNames("contract_address", "address")
    val contractAddress: Felt,
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

    @JsonNames("address")
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

    @JsonNames("starting_block_num")
    override val startingBlockNumber: Int,

    @JsonNames("current_block_hash")
    override val currentBlockHash: Felt,

    @JsonNames("current_block_num")
    override val currentBlockNumber: Int,

    @JsonNames("highest_block_hash")
    override val highestBlockHash: Felt,

    @JsonNames("highest_block_num")
    override val highestBlockNumber: Int,
) : Syncing()
