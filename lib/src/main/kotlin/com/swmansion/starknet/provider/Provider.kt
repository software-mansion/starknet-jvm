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
