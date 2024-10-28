package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContractStorageKeys(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("storage_keys")
    val storageKeys: List<Felt>,
)
