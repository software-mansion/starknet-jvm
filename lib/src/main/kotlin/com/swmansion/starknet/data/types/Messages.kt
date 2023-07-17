package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageL2ToL1(
    @JsonNames("from_address")
    val fromAddress: Felt,

    @JsonNames("to_address")
    val toAddress: Felt,

    @JsonNames("payload")
    val payload: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageL1ToL2(
    @JsonNames("from_address")
    val fromAddress: Felt,

    @JsonNames("to_address")
    val toAddress: Felt,

    @JsonNames("payload")
    val payload: List<Felt>,
)
