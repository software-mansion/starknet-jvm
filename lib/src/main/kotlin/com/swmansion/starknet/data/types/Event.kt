package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
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
