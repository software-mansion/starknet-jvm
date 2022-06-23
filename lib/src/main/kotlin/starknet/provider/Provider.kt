package starknet.provider

import starknet.data.types.CallContractResponse
import starknet.data.types.Invocation
import starknet.data.types.StarknetChainId

interface Provider {
    val chainId: StarknetChainId

    fun callContract(invokeTransaction: Invocation): Request<CallContractResponse>
}