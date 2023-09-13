package com.swmansion.starknet.provider

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.exceptions.RequestFailedException

/**
 * Provider for interacting with Starknet.
 *
 * Implementers of this interface provide methods for interacting with Starknet, for example through starknet gateway
 * api or JSON-RPC.
 */
interface Provider {
    val chainId: StarknetChainId

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockTag
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockTag: BlockTag): Request<List<Felt>>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockHash a hash of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockHash: Felt): Request<List<Felt>>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockNumber a number of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockNumber: Int): Request<List<Felt>>

    /**
     * Calls a contract deployed on Starknet.
     *
     * Calls a contract deployed on Starknet in the latest block.
     *
     * @param call a call to be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call): Request<List<Felt>> {
        return callContract(call, BlockTag.LATEST)
    }

    /**
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     * @param blockTag The tag of the requested block.
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
     * Get a value of storage var.
     *
     * Get a value of a storage variable of contract at the provided address and in the latest block.
     *
     * @param contractAddress an address of the contract
     * @param key an address of the storage variable inside contract
     *
     * @throws RequestFailedException
     */
    fun getStorageAt(contractAddress: Felt, key: Felt): Request<Felt> {
        return getStorageAt(contractAddress, key, BlockTag.LATEST)
    }

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
    fun invokeFunction(payload: InvokeTransactionPayload): Request<InvokeFunctionResponse>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given hash.
     *
     * @param classHash The hash of the requested contract class.
     *
     * @throws RequestFailedException
     */
    fun getClass(classHash: Felt): Request<ContractClassBase>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockHash The hash of the requested block.
     *
     * @throws RequestFailedException
     */
    fun getClassHashAt(contractAddress: Felt, blockHash: Felt): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockNumber The number of the requested block.
     *
     * @throws RequestFailedException
     */
    fun getClassHashAt(contractAddress: Felt, blockNumber: Int): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockTag The tag of the requested block.
     *
     * @throws RequestFailedException
     */
    fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): Request<Felt>

    /**
     * Get the contract class hash.
     *
     * Get the contract class hash in the given block for the contract deployed at the given address and in the
     * latest block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     *
     * @throws RequestFailedException
     */
    fun getClassHashAt(contractAddress: Felt): Request<Felt> {
        return getClassHashAt(contractAddress, BlockTag.LATEST)
    }

    /**
     * Declare version 1 contract
     *
     * Declare a version 1 contract on Starknet.
     *
     * @param payload declare transaction version 1 payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionV1Payload): Request<DeclareResponse>

    /**
     * Declare version 2 contract
     *
     * Declare a version 2 contract on Starknet.
     *
     * @param payload declare transaction version 2 payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionV2Payload): Request<DeclareResponse>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transaction, for which the fee is to be estimated.
     * @param blockHash a hash of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(payload: List<TransactionPayload>, blockHash: Felt): Request<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transaction, for which the fee is to be estimated.
     * @param blockNumber a number of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(payload: List<TransactionPayload>, blockNumber: Int): Request<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transaction, for which the fee is to be estimated.
     * @param blockTag a tag of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(payload: List<TransactionPayload>, blockTag: BlockTag): Request<List<EstimateFeeResponse>>

    fun getEstimateFee(payload: List<TransactionPayload>): Request<List<EstimateFeeResponse>> {
        return getEstimateFee(payload, BlockTag.LATEST)
    }

    /**
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for pending block.
     *
     * @param contractAddress address of account contract
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt): Request<Felt> = getNonce(contractAddress, blockTag = BlockTag.PENDING)

    /**
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for specified block tag.
     *
     * @param contractAddress address of account contract
     * @param blockTag block tag used for returning this value
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt, blockTag: BlockTag): Request<Felt>

    /**
     * Get the block number.
     *
     * Get the most recent accepted block number.
     *
     * @throws RequestFailedException
     */
    fun getBlockNumber(): Request<Int>

    /**
     * Get the hash and number of the block.
     *
     * Get the most recent accepted block hash and number.
     *
     * @throws RequestFailedException
     */
    fun getBlockHashAndNumber(): Request<GetBlockHashAndNumberResponse>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockTag The tag of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockTag: BlockTag): Request<Int>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockHash The hash of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockHash: Felt): Request<Int>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockNumber The number of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockNumber: Int): Request<Int>

    /**
     * Deploy an account contract.
     *
     * Deploy a new account contract on Starknet.
     *
     * @param payload deploy account transaction payload
     *
     * @throws RequestFailedException
     */
    fun deployAccount(payload: DeployAccountTransactionPayload): Request<DeployAccountResponse>
}
