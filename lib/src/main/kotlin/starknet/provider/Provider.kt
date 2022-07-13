package starknet.provider

import starknet.data.types.*

/**
 * Provider for interacting with StarkNet.
 *
 * Implementers of this interface provide methods for interacting with StarkNet, for example through starknet gateway
 * api or JSON-RPC.
 */
interface Provider {
    val chainId: StarknetChainId

    /**
     * Calls a contract deployed on StarkNet.
     *
     * @param call a call to be made
     * @param blockTag
     */
    fun callContract(call: Call, blockTag: BlockTag): Request<CallContractResponse>

    /**
     * Calls a contract deployed on StarkNet.
     *
     * @param call a call to be made
     * @param blockHash a hash of the block in respect to what the call will be made
     */
    fun callContract(call: Call, blockHash: Felt): Request<CallContractResponse>

    /**
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     * @param blockTag
     */
    fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<GetStorageAtResponse>

    /**
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     * @param blockHash a hash of the block in respect to what the query will be made
     */
    fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<GetStorageAtResponse>

    /**
     * Invoke a function.
     *
     * Invoke a function in deployed contract.
     *
     * @param payload invoke function payload
     */
    fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse>
}