package com.swmansion.starknet.data.responses

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageToL1(
    @JsonNames("to_address")
    val toAddress: Felt,

    @JsonNames("payload")
    val payload: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MessageToL2(
    @JsonNames("from_address")
    val fromAddress: Felt,

    @JsonNames("payload")
    val payload: List<Felt>,
)
