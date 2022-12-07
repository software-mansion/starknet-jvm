package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.transactions.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CallContractPayload(
    @SerialName("request")
    val request: Call,

    @SerialName("block_id")
    val blockId: BlockId,
)

@Serializable
data class GetStorageAtPayload(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("key")
    val key: Felt,

    @SerialName("block_id")
    val blockId: BlockId,
)

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
data class EstimateInvokeFunctionFeePayload(
        @SerialName("request")
        val request: InvokeTransactionPayload,

        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class EstimateDeployTransactionFeePayload(
        @SerialName("request")
        val request: DeployTransactionPayload,

        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class EstimateDeployAccountTransactionFeePayload(
        @SerialName("request")
        val request: DeployAccountTransactionPayload,

        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class EstimateDeclareTransactionFeePayload(
        @SerialName("request")
        val request: DeclareTransactionPayload,

        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class GetBlockTransactionCountPayload(
    @SerialName("block_id")
    val blockId: BlockId,
)

@Serializable
data class GetNoncePayload(
        @SerialName("contract_address")
        val contractAddress: Felt,

        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class GetBlockWithTransactionsPayload(
        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class GetStateUpdatePayload(
        @SerialName("block_id")
        val blockId: BlockId,
)

@Serializable
data class GetTreansactionByBlockIdAndIndexPayload(
        @SerialName("block_id")
        val blockId: BlockId,

        @SerialName("index")
        val index: Int,
)

