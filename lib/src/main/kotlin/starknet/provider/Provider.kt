package starknet.provider

import starknet.data.types.CallContractResponse
import starknet.data.types.Invocation

interface Provider {
    fun callContract(invokeTransaction: Invocation): Request<CallContractResponse>
}