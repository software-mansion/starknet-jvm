package com.swmansion.starknet.provider

import com.swmansion.starknet.data.responses.CommonTransactionReceipt
import com.swmansion.starknet.data.responses.Transaction
import com.swmansion.starknet.data.types.*

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
    fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<Felt>

    /**
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     * @param blockHash a hash of the block in respect to what the query will be made
     */
    fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<Felt>

    /**
     * Get a transaction.
     *
     * Get the details of a submitted transaction.
     *
     * @param transactionHash a hash of sent transaction
     */
    fun getTransaction(transactionHash: Felt): Request<Transaction>

    /**
     * Get transaction receipt
     *
     * Get a receipt of the transactions.
     *
     * @param transactionHash a hash of sent transaction
     */
    fun getTransactionReceipt(transactionHash: Felt): Request<out CommonTransactionReceipt>

    /**
     * Invoke a function.
     *
     * Invoke a function in deployed contract.
     *
     * @param payload invoke function payload
     */
    fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given hash.
     *
     * @param classHash The hash of the requested contract class.
     */
    fun getClass(classHash: Felt): Request<ContractClass>

    fun getClassHashAt(blockHash: Felt, contractAddress: Felt): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param blockNumber The number of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassHashAt(blockNumber: Int, contractAddress: Felt): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param blockTag The tag of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassHashAt(blockTag: BlockTag, contractAddress: Felt): Request<Felt>

    /**
     * Deploy a contract
     *
     * Deploy a contract on StarkNet.
     *
     * @param payload deploy transaction payload
     */
    fun deployContract(payload: DeployTransactionPayload): Request<DeployResponse>

    /**
     * Declare contract
     *
     * Declare a contract on StarkNet.
     *
     * @param payload declare transaction payload
     */
    fun declareContract(payload: DeclareTransactionPayload): Request<DeclareResponse>
}
