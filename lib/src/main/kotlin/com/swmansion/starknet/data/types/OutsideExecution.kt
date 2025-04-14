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

interface OutsideExecution : ConvertibleToCalldata {
    val caller: Felt
    val nonce: Felt
    val executeAfter: Felt
    val executeBefore: Felt
    val calls: List<OutsideCall>

    fun toTypedData(chainId: StarknetChainId): TypedData
}

@Serializable
data class OutsideExecutionV2(
    @SerialName("Caller")
    override val caller: Felt,

    @SerialName("Nonce")
    override val nonce: Felt,

    @SerialName("Execute After")
    override val executeAfter: Felt,

    @SerialName("Execute Before")
    override val executeBefore: Felt,

    @SerialName("Calls")
    override val calls: List<OutsideCallV2>,
) : OutsideExecution {
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

    override fun toTypedData(chainId: StarknetChainId): TypedData {
        val domain = domain(chainId)

        val outsideExecutionTypesV2 = listOf(
            Domain.typeDescriptorV1,
            typeDescriptor,
            OutsideCallV2.typeDescriptor,
        )

        return TypedData(
            types = outsideExecutionTypesV2.associate { it.name to it.properties },
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