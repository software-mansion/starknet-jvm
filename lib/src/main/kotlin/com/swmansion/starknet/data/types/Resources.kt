package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface Resources {
    val l1Gas: UInt
    val l2Gas: UInt
}

@Serializable
class InnerCallExecutionResources(
    @SerialName("l1_gas")
    override val l1Gas: UInt,

    @SerialName("l2_gas")
    override val l2Gas: UInt,
) : Resources

@Serializable
data class ExecutionResources(
    @SerialName("l1_gas")
    override val l1Gas: UInt,

    @SerialName("l1_data_gas")
    val l1DataGas: UInt,

    @SerialName("l2_gas")
    override val l2Gas: UInt,
) : Resources
