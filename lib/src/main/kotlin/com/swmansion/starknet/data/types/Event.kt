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
data class EventContent(
    @JsonNames("keys")
    val keys: List<Felt>,

    @JsonNames("data")
    val data: List<Felt>,
)

@Serializable
data class OrderedEvent(
    @SerialName("order")
    val order: Int,

    @SerialName("keys")
    val keys: List<Felt>,

    @SerialName("data")
    val data: List<Felt>,
)

@Serializable
data class EmittedEvent @JvmOverloads constructor(
    @SerialName("from_address")
    val address: Felt,

    @SerialName("keys")
    val keys: List<Felt>,

    @SerialName("data")
    val data: List<Felt>,

    @SerialName("block_hash")
    val blockHash: Felt? = null,

    @SerialName("block_number")
    val blockNumber: Int? = null,

    @SerialName("transaction_hash")
    val transactionHash: Felt,
)

@Serializable
data class GetEventsPayload @JvmOverloads constructor(
    @SerialName("from_block")
    val fromBlockId: BlockId? = null,

    @SerialName("to_block")
    val toBlockId: BlockId? = null,

    @SerialName("address")
    val address: Felt? = null,

    @SerialName("keys")
    val keys: List<List<Felt>>? = null,

    @SerialName("chunk_size")
    val chunkSize: Int,

    @SerialName("continuation_token")
    val continuationToken: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GetEventsResult @JvmOverloads constructor(
    @JsonNames("events")
    val events: List<EmittedEvent>,

    @JsonNames("continuation_token")
    val continuationToken: String? = null,
) : StarknetResponse
