package starknet.provider

import starknet.data.responses.Transaction
import starknet.data.responses.TransactionReceipt
import starknet.data.types.BlockTag
import starknet.data.types.Call
import starknet.data.types.CallContractResponse
import starknet.data.types.Felt
import starknet.data.types.InvokeFunctionPayload
import starknet.data.types.InvokeFunctionResponse
import starknet.data.types.StarknetChainId

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
    fun getTransactionReceipt(transactionHash: Felt): Request<TransactionReceipt>

    /**
     * Invoke a function.
     *
     * Invoke a function in deployed contract.
     *
     * @param payload invoke function payload
     */
    fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse>
}
