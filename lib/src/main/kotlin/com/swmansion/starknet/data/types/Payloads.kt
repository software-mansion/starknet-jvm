package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed class PayloadWithBlockId {
    abstract val blockId: BlockId
}

@Serializable
internal data class CallContractPayload(
    @SerialName("request")
    val request: Call,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetStorageAtPayload(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("key")
    val key: Felt,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetTransactionByHashPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
internal data class GetTransactionReceiptPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
internal data class GetTransactionStatusPayload(
    @SerialName("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
internal data class GetMessagesStatusPayload(
    @SerialName("transaction_hash")
    val transactionHash: NumAsHex,
)

@Serializable
internal data class EstimateTransactionFeePayload(
    @SerialName("request")
    val request: List<ExecutableTransaction>,

    @SerialName("simulation_flags")
    val simulationFlags: Set<SimulationFlagForEstimateFee>,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class EstimateMessageFeePayload(
    @SerialName("message")
    val message: MessageL1ToL2,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetBlockTransactionCountPayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetNoncePayload(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetStorageProofPayload constructor(
    @SerialName("block_id")
    val blockId: BlockId,

    @SerialName("class_hashes")
    val classHashes: List<Felt>? = null,

    @SerialName("contract_addresses")
    val contractAddresses: List<Felt>? = null,

    @SerialName("contracts_storage_keys")
    val contractsStorageKeys: List<ContractsStorageKeys>? = null,
)

@Serializable
internal data class GetBlockWithTransactionsPayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetBlockWithTransactionHashesPayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetBlockWithReceiptsPayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetStateUpdatePayload(
    @SerialName("block_id")
    override val blockId: BlockId,
) : PayloadWithBlockId()

@Serializable
internal data class GetTransactionByBlockIdAndIndexPayload(
    @SerialName("block_id")
    override val blockId: BlockId,

    @SerialName("index")
    val index: Int,
) : PayloadWithBlockId()

@Serializable
internal data class SimulateTransactionsPayload(
    @SerialName("transactions")
    val transactions: List<
        ExecutableTransaction,
        >,

    @SerialName("block_id")
    override val blockId: BlockId,

    @SerialName("simulation_flags")
    val simulationFlags: Set<SimulationFlag>,
) : PayloadWithBlockId()
