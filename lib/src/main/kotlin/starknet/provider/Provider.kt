package starknet.provider

import starknet.data.types.*

interface Provider {
    val chainId: StarknetChainId

    fun callContract(payload: CallContractPayload): Request<CallContractResponse>

    fun getStorageAt(payload: GetStorageAtPayload): Request<GetStorageAtResponse>

    fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse>
}