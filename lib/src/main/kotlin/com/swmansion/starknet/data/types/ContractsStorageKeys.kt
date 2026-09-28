package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class StorageKey(val value: String) {
    init {
        require(REGEX.matches(value)) {
            "Invalid storage key format: $value. Must match regex: ${REGEX.pattern}"
        }
    }

    companion object {
        private val REGEX = Regex("^0x(0|[0-7]{1}[a-fA-F0-9]{0,62})$")
    }

    override fun toString(): String = value
}

@Serializable
data class ContractsStorageKeys(
    @SerialName("contract_address")
    val contractAddress: Felt,

    @SerialName("storage_keys")
    val storageKeys: List<StorageKey>,
)
