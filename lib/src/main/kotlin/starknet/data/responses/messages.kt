package starknet.data.responses

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import starknet.data.types.Felt

@Serializable
data class MessageToL1 @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("to_address")
    val toAddress: Felt,

    @JsonNames("payload")
    val payload: List<Felt>
)

@Serializable
data class MessageToL2 @OptIn(ExperimentalSerializationApi::class) constructor(
    @JsonNames("from_address")
    val fromAddress: Felt,

    @JsonNames("payload")
    val payload: List<Felt>
)
