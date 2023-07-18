package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PayloadWithBlockId {
    abstract val blockId: BlockId
}

@Serializable
data class CallContractPayload(
    @SerialName("request")
    val request: Call,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetStorageAtPayload(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("key")
    val key: Felt,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetTransactionByHashPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
data class GetTransactionReceiptPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
data class EstimateTransactionFeePayload(
    @SerialName("request")
    val request: List<TransactionPayload>,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class EstimateMessageFeePayload(
    @SerialName("message")
    val message: MessageL1ToL2,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetBlockTransactionCountPayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetNoncePayload(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetBlockWithTransactionsPayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetStateUpdatePayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
data class GetTransactionByBlockIdAndIndexPayload(
    @SerialName("block_id")
    override val blockId: BlockId,

    @SerialName("index")
    val index: Int,
) : PayloadWithBlockId()

@Serializable
data class SimulateTransactionsPayload(
    @SerialName("transactions")
    val transactions: List<TransactionPayload>,

    @SerialName("block_id")
    override val blockId: BlockId,

    @SerialName("simulation_flags")
    val simulationFlags: Set<SimulationFlag>,
) : PayloadWithBlockId()
