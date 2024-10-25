package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Resources {
    abstract val l1Gas: Int
    abstract val l2Gas: Int
}

@Serializable
data class ExecutionResources(
    @SerialName("l1_gas")
    override val l1Gas: Int,

    @SerialName("l1_data_gas")
    val l1DataGas: Int,

    @SerialName("l2_gas")
    override val l2Gas: Int,
) : Resources()

@Serializable
data class InnerCallExecutionResources(
    @SerialName("l1_gas")
    override val l1Gas: Int,

    @SerialName("l2_gas")
    override val l2Gas: Int,
) : Resources()
