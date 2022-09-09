package com.swmansion.starknet.provider

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.exceptions.RequestFailedException

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
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockTag: BlockTag): Request<List<Felt>>

    /**
     * Calls a contract deployed on StarkNet.
     *
     * @param call a call to be made
     * @param blockHash a hash of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockHash: Felt): Request<List<Felt>>

    /**
     * Calls a contract deployed on StarkNet.
     *
     * @param call a call to be made
     * @param blockNumber a number of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockNumber: Int): Request<List<Felt>>

    /**
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     * @param blockTag
     *
     * @throws RequestFailedException
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
     *
     * @throws RequestFailedException
     */
    fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<Felt>

    /**
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     * @param blockNumber a number of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getStorageAt(contractAddress: Felt, key: Felt, blockNumber: Int): Request<Felt>

    /**
     * Get a transaction.
     *
     * Get the details of a submitted transaction.
     *
     * @param transactionHash a hash of sent transaction
     *
     * @throws RequestFailedException
     */
    fun getTransaction(transactionHash: Felt): Request<Transaction>

    /**
     * Get transaction receipt
     *
     * Get a receipt of the transactions.
     *
     * @param transactionHash a hash of sent transaction
     *
     * @throws RequestFailedException
     */
    fun getTransactionReceipt(transactionHash: Felt): Request<out TransactionReceipt>

    /**
     * Invoke a function.
     *
     * Invoke a function in deployed contract.
     *
     * @param payload invoke function payload
     *
     * @throws RequestFailedException
     */
    fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given hash.
     *
     * @param classHash The hash of the requested contract class.
     *
     * @throws RequestFailedException
     */
    fun getClass(classHash: Felt): Request<ContractClass>

    fun getClassHashAt(contractAddress: Felt, blockHash: Felt): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param blockNumber The number of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     *
     * @throws RequestFailedException
     */
    fun getClassHashAt(contractAddress: Felt, blockNumber: Int): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param blockTag The tag of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     *
     * @throws RequestFailedException
     */
    fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): Request<Felt>

    /**
     * Deploy a contract
     *
     * Deploy a contract on StarkNet.
     *
     * @param payload deploy transaction payload
     *
     * @throws RequestFailedException
     */
    fun deployContract(payload: DeployTransactionPayload): Request<DeployResponse>

    /**
     * Declare contract
     *
     * Declare a contract on StarkNet.
     *
     * @param payload declare transaction payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionPayload): Request<DeclareResponse>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction.
     *
     * @param request invoke transaction, for which the fee is to be estimated.
     * @param blockHash a hash of the block in respect to what the query will be made
     */
    fun getEstimateFee(request: InvokeTransaction, blockHash: Felt): Request<EstimateFeeResponse>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction.
     *
     * @param request invoke transaction, for which the fee is to be estimated.
     * @param blockNumber a number of the block in respect to what the query will be made
     */
    fun getEstimateFee(request: InvokeTransaction, blockNumber: Int): Request<EstimateFeeResponse>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction.
     *
     * @param request invoke transaction, for which the fee is to be estimated.
     * @param blockTag a tag of the block in respect to what the query will be made
     */
    fun getEstimateFee(request: InvokeTransaction, blockTag: BlockTag): Request<EstimateFeeResponse>
}
