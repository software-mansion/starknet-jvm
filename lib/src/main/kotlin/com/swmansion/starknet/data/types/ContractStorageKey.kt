package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContractStorageKey(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("key")
    val key: Felt,
)
