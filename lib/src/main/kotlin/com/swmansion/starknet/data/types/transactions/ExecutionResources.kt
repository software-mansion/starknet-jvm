package com.swmansion.starknet.data.types.transactions

import com.swmansion.starknet.data.types.NumAsHex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionResources(
    @SerialName("steps")
    val steps: NumAsHex,

    @SerialName("memory_holes")
    val memoryHoles: NumAsHex? = null,

    @SerialName("range_check_builtin_applications")
    val rangeCheckApplications: NumAsHex,

    @SerialName("pedersen_builtin_applications")
    val pedersenApplications: NumAsHex,

    @SerialName("poseidon_builtin_applications")
    val poseidonApplications: NumAsHex,

    @SerialName("ec_op_builtin_applications")
    val ecOpApplications: NumAsHex,

    @SerialName("ecdsa_builtin_applications")
    val ecdsaApplications: NumAsHex,

    @SerialName("bitwise_builtin_applications")
    val bitwiseApplications: NumAsHex,

    @SerialName("keccak_builtin_applications")
    val keccakApplications: NumAsHex,
)
