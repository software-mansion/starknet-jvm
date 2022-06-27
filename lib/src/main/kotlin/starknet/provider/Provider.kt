package starknet.provider

import starknet.data.types.*

interface Provider {
    val chainId: StarknetChainId

    fun callContract(call: Call, callParams: CallExtraParams): Request<CallContractResponse>
}