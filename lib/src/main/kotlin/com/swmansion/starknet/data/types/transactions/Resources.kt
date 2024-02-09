@file:JvmName("Resources")

package com.swmansion.starknet.data.types.transactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class Resources{
    abstract val steps: Int
    abstract val memoryHoles: Int?
    abstract val rangeCheckApplications: Int?
    abstract val pedersenApplications: Int?
    abstract val poseidonApplications: Int?
    abstract val ecOpApplications: Int?
    abstract val ecdsaApplications: Int?
    abstract val bitwiseApplications: Int?
    abstract val keccakApplications: Int?
    abstract val segmentArenaApplications: Int?
}

@Serializable
data class ComputationResources(
    @SerialName("steps")
    override val steps: Int,

    @SerialName("memory_holes")
    override val memoryHoles: Int? = null,

    @SerialName("range_check_builtin_applications")
    override val rangeCheckApplications: Int? = null,

    @SerialName("pedersen_builtin_applications")
    override val pedersenApplications: Int? = null,

    @SerialName("poseidon_builtin_applications")
    override val poseidonApplications: Int? = null,

    @SerialName("ec_op_builtin_applications")
    override val ecOpApplications: Int? = null,

    @SerialName("ecdsa_builtin_applications")
    override val ecdsaApplications: Int? = null,

    @SerialName("bitwise_builtin_applications")
    override val bitwiseApplications: Int? = null,

    @SerialName("keccak_builtin_applications")
    override val keccakApplications: Int? = null,

    @SerialName("segment_arena_builtin")
    override val segmentArenaApplications: Int? = null,
) : Resources()

@Serializable
data class ExecutionResources(
    @SerialName("steps")
    override val steps: Int,

    @SerialName("memory_holes")
    override val memoryHoles: Int? = null,

    @SerialName("range_check_builtin_applications")
    override val rangeCheckApplications: Int? = null,

    @SerialName("pedersen_builtin_applications")
    override val pedersenApplications: Int? = null,

    @SerialName("poseidon_builtin_applications")
    override val poseidonApplications: Int? = null,

    @SerialName("ec_op_builtin_applications")
    override val ecOpApplications: Int? = null,

    @SerialName("ecdsa_builtin_applications")
    override val ecdsaApplications: Int? = null,

    @SerialName("bitwise_builtin_applications")
    override val bitwiseApplications: Int? = null,

    @SerialName("keccak_builtin_applications")
    override val keccakApplications: Int? = null,

    @SerialName("segment_arena_builtin")
    override val segmentArenaApplications: Int? = null,

    @SerialName("data_availability")
    val dataAvailability: DataAvailability,
) : Resources()

@Serializable
data class DataAvailability(
    @SerialName("l1_gas")
    val l1Gas: Int,

    @SerialName("l1_data_gas")
    val l1DataGas: Int,
)