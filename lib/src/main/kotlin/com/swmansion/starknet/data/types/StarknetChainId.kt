package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.StarknetChainIdSerializer
import kotlinx.serialization.Serializable

@Serializable(with = StarknetChainIdSerializer::class)
data class StarknetChainId(val value: Felt) {
    companion object {
        @field:JvmField
        val MAIN = StarknetChainId(Felt.fromHex("0x534e5f4d41494e")) // encodeShortString('SN_MAIN'),

        @field:JvmField
        val GOERLI = StarknetChainId(Felt.fromHex("0x534e5f474f45524c49")) // encodeShortString('SN_GOERLI'),

        @field:JvmField
        val SEPOLIA = StarknetChainId(Felt.fromHex("0x534e5f5345504f4c4941")) // encodeShortString('SN_SEPOLIA'),

        @field:JvmField
        val INTEGRATION_SEPOLIA = StarknetChainId(Felt.fromHex("0x534e5f494e544547524154494f4e5f5345504f4c4941")) // encodeShortString('SN_INTEGRATION_SEPOLIA'),

        @JvmStatic
        fun fromNetworkName(networkName: String): StarknetChainId {
            return StarknetChainId(Felt.fromShortString(networkName))
        }

        @JvmStatic
        fun fromHex(hex: String): StarknetChainId {
            return StarknetChainId(Felt.fromHex(hex))
        }
    }
    fun toNetworkName(): String {
        return value.toShortString()
    }
}
