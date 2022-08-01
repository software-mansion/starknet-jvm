package starknet.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import starknet.data.types.Felt

@Serializable
data class Event @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("address")
    val address: Felt,

    @JsonNames("keys")
    val keys: List<Felt>,

    @JsonNames("data")
    val data: List<Felt>
)
