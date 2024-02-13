package com.swmansion.starknet.data.types

import kotlinx.serialization.Serializable

@Serializable
class StarknetChainId private constructor(val value: Felt) {
    companion object {
        @field:JvmField
        val MAIN = StarknetChainId(Felt.fromHex("0x534e5f4d41494e")) // encodeShortString('SN_MAIN'),

        @field:JvmField
        val GOERLI = StarknetChainId(Felt.fromHex("0x534e5f474f45524c49")) // encodeShortString('SN_GOERLI'),

        @field:JvmField
        val SEPOLIA = StarknetChainId(Felt.fromHex("0x534e5f5345504f4c4941")) // encodeShortString('SN_SEPOLIA'),]

        @field:JvmField
        val INTEGRATION_SEPOLIA =
            StarknetChainId(Felt.fromHex("0x534e5f494e544547524154494f4e5f5345504f4c4941")) // encodeShortString('SN_INTEGRATION_SEPOLIA'),

        @JvmStatic
        fun fromShortString(shortString: String): StarknetChainId {
            return fromFelt(Felt.fromShortString(shortString))
        }

        @JvmStatic
        fun fromHex(hex: String): StarknetChainId {
            return fromFelt(Felt.fromHex(hex))
        }

        @JvmStatic
        fun fromFelt(felt: Felt): StarknetChainId {
            return when (felt) {
                MAIN.value -> MAIN
                GOERLI.value -> GOERLI
                SEPOLIA.value -> SEPOLIA
                INTEGRATION_SEPOLIA.value -> INTEGRATION_SEPOLIA
                else -> StarknetChainId(felt)
            }
        }
    }
}
