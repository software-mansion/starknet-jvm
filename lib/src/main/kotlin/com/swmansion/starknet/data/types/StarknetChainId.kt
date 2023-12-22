package com.swmansion.starknet.data.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class StarknetChainId(val value: Felt) {
    @SerialName("0x534e5f4d41494e")
    MAINNET(Felt.fromHex("0x534e5f4d41494e")), // encodeShortString('SN_MAIN'),

    // TODO: add deprecated warning
    @SerialName("0x534e5f474f45524c49")
    GOERLI(Felt.fromHex("0x534e5f474f45524c49")), // encodeShortString('SN_GOERLI'),

    @SerialName("0x534e5f5345504f4c4941")
    SEPOLIA_TESTNET(Felt.fromHex("0x534e5f5345504f4c4941")), // encodeShortString('SN_SEPOLIA'),]

    @SerialName("0x534e5f494e544547524154494f4e5f5345504f4c4941")
    SEPOLIA_INTEGRATION(Felt.fromHex("0x534e5f494e544547524154494f4e5f5345504f4c4941")), // encodeShortString('SN_INTEGRATION_SEPOLIA'),
}
