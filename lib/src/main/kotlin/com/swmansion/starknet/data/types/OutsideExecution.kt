package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.TypedData
import com.swmansion.starknet.data.TypedData.Descriptor
import com.swmansion.starknet.data.TypedData.Domain
import com.swmansion.starknet.data.TypedData.StandardType
import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toCalldata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class OutsideExecution(
    @SerialName("Caller")
    val caller: Felt,
    @SerialName("Nonce")
    val nonce: Felt,
    @SerialName("Execute After")
    val executeAfter: Felt,
    @SerialName("Execute Before")
    val executeBefore: Felt,
    @SerialName("Calls")
    val calls: List<OutsideCall>,
) : ConvertibleToCalldata {
    companion object {
        val typeDescriptor = Descriptor(
            "OutsideExecution",
            listOf(
                StandardType(name = "Caller", type = "ContractAddress"),
                StandardType(name = "Nonce", type = "felt"),
                StandardType(name = "Execute After", type = "u128"),
                StandardType(name = "Execute Before", type = "u128"),
                StandardType(name = "Calls", type = "Call*"),
            ),
        )
    }

    override fun toCalldata(): List<Felt> {
        return listOf(
            caller.toCalldata(),
            nonce.toCalldata(),
            executeAfter.toCalldata(),
            executeBefore.toCalldata(),
            Felt(calls.size).toCalldata(),
            calls.toCalldata(),
        ).flatten()
    }

    fun toTypedData(chainId: StarknetChainId): TypedData {
        val domain = domain(chainId)

        val OutsideExecutionTypesV2 = listOf(
            Domain.typeDescriptorV1,
            typeDescriptor,
            OutsideCall.typeDescriptor,
        )

        return TypedData(
            types = OutsideExecutionTypesV2.associate { it.name to it.properties },
            primaryType = typeDescriptor.name,
            domain = Json.encodeToString(domain),
            message = Json.encodeToString(this),
        )
    }

    private fun domain(chainId: StarknetChainId): Domain {
        return Domain(
            name = JsonPrimitive("Account.execute_from_outside"),
            version = JsonPrimitive("2"),
            chainId = JsonPrimitive(chainId.value.hexString()),
            revision = JsonPrimitive("1"),
        )
    }
}
