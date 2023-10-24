package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionResources(
    @SerialName("steps")
    val steps: Felt,

    @SerialName("memory_holes")
    val memoryHoles: Felt? = null,

    @SerialName("range_check_builtin_applications")
    val rangeCheckApplications: Felt,

    @SerialName("pedersen_builtin_applications")
    val pedersenApplications: Felt,

    @SerialName("poseidon_builtin_applications")
    val poseidonApplications: Felt,

    @SerialName("ec_op_builtin_applications")
    val ecOpApplications: Felt,

    @SerialName("ecdsa_builtin_applications")
    val ecdsaApplications: Felt,

    @SerialName("bitwise_builtin_applications")
    val bitwiseApplications: Felt,

    @SerialName("keccak_builtin_applications")
    val keccakApplications: Felt,
)
