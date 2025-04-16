package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.TypedData.Descriptor
import com.swmansion.starknet.data.TypedData.StandardType
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface OutsideCall : ConvertibleToCalldata {
    val to: Felt
    val selector: Felt
    val calldata: List<Felt>
}

@Serializable
data class OutsideCallV2(
    @SerialName("To")
    override val to: Felt,

    @SerialName("Selector")
    override val selector: Felt,

    @SerialName("Calldata")
    override val calldata: List<Felt>,
) : OutsideCall {
    companion object {
        val typeDescriptor = Descriptor(
            "Call",
            listOf(
                StandardType(name = "To", type = "ContractAddress"),
                StandardType(name = "Selector", type = "selector"),
                StandardType(name = "Calldata", type = "felt*"),
            ),
        )
    }

    override fun toCalldata(): List<Felt> {
        return listOf(
            to.toCalldata(),
            selector.toCalldata(),
            listOf(Felt(calldata.size)),
            calldata,
        ).flatten()
    }
}
