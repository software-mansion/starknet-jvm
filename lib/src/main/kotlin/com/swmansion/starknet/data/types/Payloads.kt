package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.transactions.InvokeTransaction
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
data class EstimateFeePayload(
    @SerialName("request")
    val request: InvokeTransaction,

    @SerialName("block_id")
    val blockId: BlockId,
)

@Serializable
data class GetBlockTransactionCountPayload(
    @SerialName("block_id")
    val blockId: BlockId,
)
