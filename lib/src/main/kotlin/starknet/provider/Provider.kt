package starknet.provider

import starknet.data.types.*
import types.Felt

interface Provider {
    val chainId: StarknetChainId

    fun callContract(call: Call, blockTag: BlockTag): Request<CallContractResponse>
    fun callContract(call: Call, blockHash: Felt): Request<CallContractResponse>

    fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<GetStorageAtResponse>
    fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<GetStorageAtResponse>

    fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse>
}