package com.swmansion.starknet.data.types

enum class StarknetChainId(val value: Felt) {
    MAINNET(Felt.fromHex("0x534e5f4d41494e")), // encodeShortString('SN_MAIN'),
    TESTNET(Felt.fromHex("0x534e5f474f45524c49")), // encodeShortString('SN_GOERLI'),
    SEPOLIA_TESTNET(Felt.fromHex("0x534e5f5345504f4c4941")), // encodeShortString('SN_SEPOLIA'),
    SEPOLIA_INTEGRATION(Felt.fromHex("0x534e5f494e544547524154494f4e5f5345504f4c4941")), // encodeShortString('SN_INTEGRATION_SEPOLIA'),
}
