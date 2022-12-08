package com.swmansion.starknet.data.types

enum class StarknetChainId(val value: Felt) {
    MAINNET(Felt.fromHex("0x534e5f4d41494e")), // encodeShortString('SN_MAIN'),
    TESTNET(Felt.fromHex("0x534e5f474f45524c49")), // encodeShortString('SN_GOERLI'),
    TESTNET2(Felt.fromHex("0x534e5f474f45524c4932")), // encodeShortString('SN_GOERLI2'),
}
