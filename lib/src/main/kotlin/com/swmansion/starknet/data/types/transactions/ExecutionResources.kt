package com.swmansion.starknet.data.types.transactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionResources(
    @SerialName("steps")
    val steps: Int,

    @SerialName("memory_holes")
    val memoryHoles: Int? = null,

    @SerialName("range_check_builtin_applications")
    val rangeCheckApplications: Int? = null,

    @SerialName("pedersen_builtin_applications")
    val pedersenApplications: Int? = null,

    @SerialName("poseidon_builtin_applications")
    val poseidonApplications: Int? = null,

    @SerialName("ec_op_builtin_applications")
    val ecOpApplications: Int? = null,

    @SerialName("ecdsa_builtin_applications")
    val ecdsaApplications: Int? = null,

    @SerialName("bitwise_builtin_applications")
    val bitwiseApplications: Int? = null,

    @SerialName("keccak_builtin_applications")
    val keccakApplications: Int? = null,

    @SerialName("segment_arena_builtin")
    val segmentArenaApplications: Int? = null,
)
