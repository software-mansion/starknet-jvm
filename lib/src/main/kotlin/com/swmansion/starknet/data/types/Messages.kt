package com.swmansion.starknet.data.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GatewayMessageL2ToL1(
    @JsonNames("to_address")
    @SerialName("to_address")
    val toAddress: Felt,

    @SerialName("payload")
    @JsonNames("payload")
    val payload: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GatewayMessageL1ToL2(
    @JsonNames("from_address")
    @SerialName("from_address")
    val fromAddress: Felt,

    @JsonNames("payload")
    @SerialName("payload")
    val payload: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RpcMessageL2ToL1(
    @JsonNames("from_address")
    @SerialName("from_address")
    val fromAddress: Felt,

    @JsonNames("to_address")
    @SerialName("to_address")
    val toAddress: Felt,

    @JsonNames("payload")
    @SerialName("payload")
    val payload: List<Felt>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RpcMessageL1ToL2(
    @JsonNames("from_address")
    @SerialName("from_address")
    val fromAddress: Felt,

    @JsonNames("to_address")
    @SerialName("to_address")
    val toAddress: Felt,

    @JsonNames("entry_point_selector")
    @SerialName("entry_point_selector")
    val selector: Felt,

    @JsonNames("payload")
    @SerialName("payload")
    val payload: List<Felt>,
)
