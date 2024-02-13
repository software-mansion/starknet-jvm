package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class StarknetChainId(open val value: Felt) {
    @SerialName("0x534e5f4d41494e")
    data object Main : StarknetChainId(Felt.fromHex("0x534e5f4d41494e"))

    @SerialName("0x534e5f474f45524c49")
    data object Goerli : StarknetChainId(Felt.fromHex("0x534e5f474f45524c49"))

    @SerialName("0x534e5f5345504f4c4941")
    data object Sepolia : StarknetChainId(Felt.fromHex("0x534e5f5345504f4c4941"))

    @SerialName("0x534e5f494e544547524154494f4e5f5345504f4c4941")
    data object IntegrationSepolia : StarknetChainId(Felt.fromHex("0x534e5f494e544547524154494f4e5f5345504f4c4941"))

    companion object {
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
                Main.value -> Main
                Goerli.value -> Goerli
                Sepolia.value -> Sepolia
                IntegrationSepolia.value -> IntegrationSepolia
                else -> CustomStarknetChainId(felt)
            }
        }
    }
}

data class CustomStarknetChainId(override val value: Felt) : StarknetChainId(value)