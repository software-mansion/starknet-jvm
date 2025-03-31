package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface Resources {
    val l1Gas: Int
    val l2Gas: Int
}

@Serializable
class InnerCallExecutionResources(
    @SerialName("l1_gas")
    override val l1Gas: Int,

    @SerialName("l2_gas")
    override val l2Gas: Int,
) : Resources

@Serializable
data class ExecutionResources(
    @SerialName("l1_gas")
    override val l1Gas: Int,

    @SerialName("l1_data_gas")
    val l1DataGas: Int,

    @SerialName("l2_gas")
    override val l2Gas: Int,
) : Resources
