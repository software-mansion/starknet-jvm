package com.swmansion.starknet.provider

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.service.http.HttpRequest

/**
 * Provider for interacting with Starknet.
 *
 * Implementers of this interface provide methods for interacting with Starknet, for example through Starknet JSON-RPC.
 */
interface Provider {
    /**
     * Get a transaction.
     *
     * Get the details of a submitted transaction.
     *
     * @param transactionHash a hash of sent transaction
     *
     * @throws RequestFailedException
     */
    fun getTransaction(transactionHash: Felt): HttpRequest<Transaction>

    /**
     * Invoke a function using version 1 transaction.
     *
     * Invoke a function in deployed contract using version 1 transaction.
     *
     * @param payload invoke function version 1 payload
     *
     * @throws RequestFailedException
     */
    fun invokeFunction(payload: InvokeTransactionV1Payload): HttpRequest<InvokeFunctionResponse>

    /**
     * Invoke a function using version 3 transaction.
     *
     * Invoke a function in deployed contract using version 3 transaction.
     *
     * @param payload invoke function version 3 payload
     *
     * @throws RequestFailedException
     */
    fun invokeFunction(payload: InvokeTransactionV3Payload): HttpRequest<InvokeFunctionResponse>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given hash.
     *
     * @param classHash The hash of the requested contract class.
     *
     * @throws RequestFailedException
     */
    fun getClass(classHash: Felt): HttpRequest<ContractClassBase>

    /**
     * Declare contract using version 2 transaction.
     *
     * Declare a contract on Starknet using version 2 transaction.
     *
     * @param payload declare transaction version 2 payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionV2Payload): HttpRequest<DeclareResponse>

    /**
     * Declare contract using version 3 transaction.
     *
     * Declare a contract on Starknet using version 3 transaction.
     *
     * @param payload declare transaction version 3 payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionV3Payload): HttpRequest<DeclareResponse>

    /**
     * Get the block number.
     *
     * Get the most recent accepted block number.
     *
     * @throws RequestFailedException
     */
    fun getBlockNumber(): HttpRequest<Int>

    /**
     * Get the hash and number of the block.
     *
     * Get the most recent accepted block hash and number.
     *
     * @throws RequestFailedException
     */
    fun getBlockHashAndNumber(): HttpRequest<GetBlockHashAndNumberResponse>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockTag The tag of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockTag: BlockTag): HttpRequest<Int>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockHash The hash of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockHash: Felt): HttpRequest<Int>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockNumber The number of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockNumber: Int): HttpRequest<Int>

    /**
     * Deploy an account contract using version 1 transaction.
     *
     * Deploy a new account contract on Starknet.
     *
     * @param payload deploy account transaction version 1 payload
     *
     * @throws RequestFailedException
     */
    fun deployAccount(payload: DeployAccountTransactionV1Payload): HttpRequest<DeployAccountResponse>

    /**
     * Deploy an account contract using version 3 transaction.
     *
     * Deploy a new account contract on Starknet.
     *
     * @param payload deploy account transaction version 3 payload
     *
     * @throws RequestFailedException
     */
    fun deployAccount(payload: DeployAccountTransactionV3Payload): HttpRequest<DeployAccountResponse>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockTag
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockTag: BlockTag): HttpRequest<List<Felt>>

    /**
     * Get the version of the spec.
     *
     * Get the version of the Starknet JSON-RPC specification being used by the node.
     *
     * @throws RequestFailedException
     *
     */
    fun getSpecVersion(): HttpRequest<String>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockHash a hash of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockHash: Felt): HttpRequest<List<Felt>>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockNumber a number of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockNumber: Int): HttpRequest<List<Felt>>

    /**
     * Calls a contract deployed on Starknet.
     *
     * Calls a contract deployed on Starknet in the latest block.
     *
     * @param call a call to be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call): HttpRequest<List<Felt>>

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
    fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): HttpRequest<Felt>

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
    fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): HttpRequest<Felt>

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
    fun getStorageAt(contractAddress: Felt, key: Felt, blockNumber: Int): HttpRequest<Felt>

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
    fun getStorageAt(contractAddress: Felt, key: Felt): HttpRequest<Felt>

    /**
     * Get transaction receipt
     *
     * Get a receipt of the transactions.
     *
     * @param transactionHash a hash of sent transaction
     *
     * @throws RequestFailedException
     */
    fun getTransactionReceipt(transactionHash: Felt): HttpRequest<out TransactionReceipt>

    /**
     * Get transaction status
     *
     * Get a status of the transaction.
     *
     * @param transactionHash a hash of sent transaction
     *
     * @throws RequestFailedException
     */

    fun getTransactionStatus(transactionHash: Felt): HttpRequest<GetTransactionStatusResponse>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given hash.
     *
     * @param classHash The hash of the requested contract class.
     * @param blockHash The hash of requested block.
     *
     * @throws RequestFailedException
     */
    fun getClass(classHash: Felt, blockHash: Felt): HttpRequest<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given block number.
     *
     * @param classHash The hash of the requested contract class.
     * @param blockNumber The number of requested block.
     *
     * @throws RequestFailedException
     */
    fun getClass(classHash: Felt, blockNumber: Int): HttpRequest<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition associated with the given block tag.
     *
     * @param classHash The hash of the requested contract class.
     * @param blockTag The tag of requested block.
     *
     * @throws RequestFailedException
     */
    fun getClass(classHash: Felt, blockTag: BlockTag): HttpRequest<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockHash The hash of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockHash: Felt): HttpRequest<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockNumber The number of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockNumber: Int): HttpRequest<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockTag The tag of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockTag: BlockTag): HttpRequest<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition in the at the given address in the latest block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassAt(contractAddress: Felt): HttpRequest<ContractClassBase>

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
    fun getClassHashAt(contractAddress: Felt, blockHash: Felt): HttpRequest<Felt>

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
    fun getClassHashAt(contractAddress: Felt, blockNumber: Int): HttpRequest<Felt>

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
    fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): HttpRequest<Felt>

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
    fun getClassHashAt(contractAddress: Felt): HttpRequest<Felt>

    /**
     * Get events
     *
     * Returns all events matching the given filter
     *
     * @param payload get events payload
     * @return GetEventsResult with list of emitted events and additional page info
     *
     * @throws RequestFailedException
     */
    fun getEvents(payload: GetEventsPayload): HttpRequest<GetEventsResult>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param blockHash a hash of the block in respect to what the query will be made
     * @param simulationFlags set of flags to be used for simulation.
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockHash: Felt,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param blockHash a hash of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(payload: List<TransactionPayload>, blockHash: Felt): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param simulationFlags set of flags to be used for simulation.
     * @param blockNumber a number of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockNumber: Int,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param blockNumber a number of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(payload: List<TransactionPayload>, blockNumber: Int): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param simulationFlags set of flags to be used for simulation.
     * @param blockTag a tag of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param blockTag a tag of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateFee(payload: List<TransactionPayload>, blockTag: BlockTag): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list for pending block.
     *
     * @param payload transactions, for which the fee is to be estimated.
     * @param simulationFlags set of flags to be used for simulation
     *
     * @throws RequestFailedException
     */

    fun getEstimateFee(payload: List<TransactionPayload>, simulationFlags: Set<SimulationFlagForEstimateFee>): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list for pending block.
     *
     * @param payload transaction, for which the fee is to be estimated.
     *
     * @throws RequestFailedException
     */

    fun getEstimateFee(payload: List<TransactionPayload>): HttpRequest<List<EstimateFeeResponse>>

    /**
     * Estimate a message fee.
     *
     * Estimate the L2 fee of a provided message sent on L1.
     *
     * @param message message, for which the fee is to be estimated.
     * @param blockHash a hash of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateMessageFee(message: MessageL1ToL2, blockHash: Felt): HttpRequest<EstimateFeeResponse>

    /**
     * Estimate a message fee.
     *
     * Estimate the L2 fee of a provided message sent on L1.
     *
     * @param message message, for which the fee is to be estimated.
     * @param blockNumber a number of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateMessageFee(message: MessageL1ToL2, blockNumber: Int): HttpRequest<EstimateFeeResponse>

    /**
     * Estimate a message fee.
     *
     * Estimate the L2 fee of a provided message sent on L1.
     *
     * @param message message, for which the fee is to be estimated.
     * @param blockTag a tag of the block in respect to what the query will be made
     *
     * @throws RequestFailedException
     */
    fun getEstimateMessageFee(message: MessageL1ToL2, blockTag: BlockTag): HttpRequest<EstimateFeeResponse>

    /**
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for pending block.
     *
     * @param contractAddress address of account contract
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt): HttpRequest<Felt>

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
    fun getNonce(contractAddress: Felt, blockTag: BlockTag): HttpRequest<Felt>

    /**
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for specified block number.
     *
     * @param contractAddress address of account contract
     * @param blockNumber block number used for returning this value
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt, blockNumber: Int): HttpRequest<Felt>

    /**
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for specified block hash.
     *
     * @param contractAddress address of account contract
     * @param blockHash block hash used for returning this value
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt, blockHash: Felt): HttpRequest<Felt>

    /**
     * Get the block synchronization status.
     *
     * Get the starting, current and highest block info or boolean indicating syncing is not in progress.
     *
     * @throws RequestFailedException
     */
    fun getSyncing(): HttpRequest<Syncing>

    /**
     * Get the chain id.
     *
     * Get the currently configured Starknet chain id.
     *
     * @throws RequestFailedException
     */
    fun getChainId(): HttpRequest<StarknetChainId>

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockTag: BlockTag): HttpRequest<BlockWithTransactions>

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockHash: Felt): HttpRequest<BlockWithTransactions>

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockNumber: Int): HttpRequest<BlockWithTransactions>

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockTag: BlockTag): HttpRequest<BlockWithTransactionHashes>

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockHash: Felt): HttpRequest<BlockWithTransactionHashes>

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockNumber: Int): HttpRequest<BlockWithTransactionHashes>

    /**
     * Get a block with transaction receipts.
     *
     * Get block information with transaction receipts given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithReceipts(blockTag: BlockTag): HttpRequest<BlockWithReceipts>

    /**
     * Get a block with transaction receipts.
     *
     * Get block information with transaction receipts given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithReceipts(blockHash: Felt): HttpRequest<BlockWithReceipts>

    /**
     * Get a block with transaction receipts.
     *
     * Get block information with transaction receipts given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithReceipts(blockNumber: Int): HttpRequest<BlockWithReceipts>

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockTag: BlockTag): HttpRequest<StateUpdate>

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockHash: Felt): HttpRequest<StateUpdate>

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockNumber: Int): HttpRequest<StateUpdate>

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockTag: BlockTag, index: Int): HttpRequest<Transaction>

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockHash: Felt, index: Int): HttpRequest<Transaction>

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockNumber: Int, index: Int): HttpRequest<Transaction>

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockTag tag of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation
     * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockTag: BlockTag, simulationFlags: Set<SimulationFlag>): HttpRequest<List<SimulatedTransaction>>

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockNumber number of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation
     * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockNumber: Int, simulationFlags: Set<SimulationFlag>): HttpRequest<List<SimulatedTransaction>>

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockHash hash of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation
     * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockHash: Felt, simulationFlags: Set<SimulationFlag>): HttpRequest<List<SimulatedTransaction>>
}
