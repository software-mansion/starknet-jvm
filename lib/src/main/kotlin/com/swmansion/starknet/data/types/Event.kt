package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Event(
    @JsonNames("address", "from_address")
    val address: Felt,

    @JsonNames("keys")
    val keys: List<Felt>,

    @JsonNames("data")
    val data: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class EmittedEvent(
    @JsonNames("address", "from_address")
    val address: Felt,

    @JsonNames("keys")
    val keys: List<Felt>,

    @JsonNames("data")
    val data: List<Felt>,

    @JsonNames("block_hash")
    val blockHash: Felt,

    @JsonNames("block_number")
    val blockNumber: Int,

    @JsonNames("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
data class GetEventsPayload(
    @SerialName("from_block")
    val fromBlockId: BlockId,

    @SerialName("to_block")
    val toBlockId: BlockId,

    @SerialName("address")
    val address: Felt,

    @SerialName("keys")
    val keys: List<Felt>,

    @SerialName("chunk_size")
    val chunkSize: Int,

    @SerialName("continuation_token")
    val continuationToken: String? = "0",
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GetEventsResult(
    @JsonNames("events")
    val events: List<EmittedEvent>,

    @JsonNames("continuation_token")
    val continuationToken: String? = "0",
)
