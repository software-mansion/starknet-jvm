package starknet.provider

import starknet.data.types.StarknetChainId

interface Provider {
    val chainId: StarknetChainId
}