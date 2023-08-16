package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageL2ToL1(
    @JsonNames("from_address")
    @SerialName("from_address")
    val fromAddress: Felt,

    @JsonNames("to_address")
    @SerialName("to_address")
    val toAddress: Felt,

    @SerialName("payload")
    @JsonNames("payload")
    val payload: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageL1ToL2(
    @JsonNames("from_address")
    @SerialName("from_address")
    val fromAddress: Felt,

    @JsonNames("to_address")
    @SerialName("to_address")
    val toAddress: Felt,

    @JsonNames("selector", "entry_point_selector")
    @SerialName("entry_point_selector")
    val selector: Felt,

    @JsonNames("nonce")
    @SerialName("nonce")
    val nonce: Felt? = null,

    @JsonNames("payload")
    @SerialName("payload")
    val payload: List<Felt>,
)
