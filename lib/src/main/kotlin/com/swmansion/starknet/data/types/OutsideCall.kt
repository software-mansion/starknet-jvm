package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.TypedData.Descriptor
import com.swmansion.starknet.data.TypedData.StandardType
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutsideCall(
    @SerialName("To")
    val to: Felt,

    @SerialName("Selector")
    val selector: Felt,

    @SerialName("Calldata")
    val calldata: List<Felt>,
) : ConvertibleToCalldata {
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
