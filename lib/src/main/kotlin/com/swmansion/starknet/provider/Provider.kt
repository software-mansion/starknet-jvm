package com.swmansion.starknet.provider

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.exceptions.RequestFailedException

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
    fun getTransaction(transactionHash: Felt): Request<Transaction>

    /**
     * Invoke a function using version 1 transaction.
     *
     * Invoke a function in deployed contract using version 1 transaction.
     *
     * @param payload invoke function version 1 payload
     *
     * @throws RequestFailedException
     */
    fun invokeFunction(payload: InvokeTransactionV1Payload): Request<InvokeFunctionResponse>

    /**
     * Invoke a function using version 3 transaction.
     *
     * Invoke a function in deployed contract using version 3 transaction.
     *
     * @param payload invoke function version 3 payload
     *
     * @throws RequestFailedException
     */
    fun invokeFunction(payload: InvokeTransactionV3Payload): Request<InvokeFunctionResponse>

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
     * Declare contract using version 2 transaction.
     *
     * Declare a contract on Starknet using version 2 transaction.
     *
     * @param payload declare transaction version 2 payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionV2Payload): Request<DeclareResponse>

    /**
     * Declare contract using version 3 transaction.
     *
     * Declare a contract on Starknet using version 3 transaction.
     *
     * @param payload declare transaction version 3 payload
     *
     * @throws RequestFailedException
     */
    fun declareContract(payload: DeclareTransactionV3Payload): Request<DeclareResponse>

    /**
     * Get the block number.
     *
     * Get the most recent accepted block number.
     *
     * @throws RequestFailedException
     */
    fun getBlockNumber(): Request<IntResponse>

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
    fun getBlockTransactionCount(blockTag: BlockTag): Request<IntResponse>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockHash The hash of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockHash: Felt): Request<IntResponse>

    /**
     * Get the block transaction count.
     *
     * Get the number of transactions in a given block.
     *
     * @param blockNumber The number of the block.
     * @throws RequestFailedException
     */
    fun getBlockTransactionCount(blockNumber: Int): Request<IntResponse>

    /**
     * Deploy an account contract using version 1 transaction.
     *
     * Deploy a new account contract on Starknet.
     *
     * @param payload deploy account transaction version 1 payload
     *
     * @throws RequestFailedException
     */
    fun deployAccount(payload: DeployAccountTransactionV1Payload): Request<DeployAccountResponse>

    /**
     * Deploy an account contract using version 3 transaction.
     *
     * Deploy a new account contract on Starknet.
     *
     * @param payload deploy account transaction version 3 payload
     *
     * @throws RequestFailedException
     */
    fun deployAccount(payload: DeployAccountTransactionV3Payload): Request<DeployAccountResponse>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockTag
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockTag: BlockTag): Request<FeltArray>

    /**
     * Get the version of the spec.
     *
     * Get the version of the Starknet JSON-RPC specification being used by the node.
     *
     * @throws RequestFailedException
     *
     */
    fun getSpecVersion(): Request<StringResponse>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockHash a hash of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockHash: Felt): Request<FeltArray>

    /**
     * Calls a contract deployed on Starknet.
     *
     * @param call a call to be made
     * @param blockNumber a number of the block in respect to what the call will be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call, blockNumber: Int): Request<FeltArray>

    /**
     * Calls a contract deployed on Starknet.
     *
     * Calls a contract deployed on Starknet in the latest block.
     *
     * @param call a call to be made
     *
     * @throws RequestFailedException
     */
    fun callContract(call: Call): Request<FeltArray>

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
    fun getStorageAt(contractAddress: Felt, key: Felt): Request<Felt>

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
     * Get transaction status
     *
     * Get a status of the transaction.
     *
     * @param transactionHash a hash of sent transaction
     *
     * @throws RequestFailedException
     */

    fun getTransactionStatus(transactionHash: Felt): Request<GetTransactionStatusResponse>

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
    fun getClass(classHash: Felt, blockHash: Felt): Request<ContractClassBase>

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
    fun getClass(classHash: Felt, blockNumber: Int): Request<ContractClassBase>

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
    fun getClass(classHash: Felt, blockTag: BlockTag): Request<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockHash The hash of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockHash: Felt): Request<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockNumber The number of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockNumber: Int): Request<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockTag The tag of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockTag: BlockTag): Request<ContractClassBase>

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition in the at the given address in the latest block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassAt(contractAddress: Felt): Request<ContractClassBase>

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
    fun getClassHashAt(contractAddress: Felt): Request<Felt>

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
    fun getEvents(payload: GetEventsPayload): Request<GetEventsResult>

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
    ): Request<EstimateFeeResponseList>

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
    fun getEstimateFee(payload: List<TransactionPayload>, blockHash: Felt): Request<EstimateFeeResponseList>

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
    ): Request<EstimateFeeResponseList>

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
    fun getEstimateFee(payload: List<TransactionPayload>, blockNumber: Int): Request<EstimateFeeResponseList>

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
    ): Request<EstimateFeeResponseList>

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
    fun getEstimateFee(payload: List<TransactionPayload>, blockTag: BlockTag): Request<EstimateFeeResponseList>

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

    fun getEstimateFee(payload: List<TransactionPayload>, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<EstimateFeeResponseList>

    /**
     * Estimate a fee.
     *
     * Estimate a fee for a provided transaction list for pending block.
     *
     * @param payload transaction, for which the fee is to be estimated.
     *
     * @throws RequestFailedException
     */

    fun getEstimateFee(payload: List<TransactionPayload>): Request<EstimateFeeResponseList>

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
    fun getEstimateMessageFee(message: MessageL1ToL2, blockHash: Felt): Request<EstimateFeeResponse>

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
    fun getEstimateMessageFee(message: MessageL1ToL2, blockNumber: Int): Request<EstimateFeeResponse>

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
    fun getEstimateMessageFee(message: MessageL1ToL2, blockTag: BlockTag): Request<EstimateFeeResponse>

    /**
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for pending block.
     *
     * @param contractAddress address of account contract
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt): Request<Felt>

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
     * Get a nonce.
     *
     * Get a nonce of an account contract of a given address for specified block number.
     *
     * @param contractAddress address of account contract
     * @param blockNumber block number used for returning this value
     *
     * @throws RequestFailedException
     */
    fun getNonce(contractAddress: Felt, blockNumber: Int): Request<Felt>

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
    fun getNonce(contractAddress: Felt, blockHash: Felt): Request<Felt>

    /**
     * Get the block synchronization status.
     *
     * Get the starting, current and highest block info or boolean indicating syncing is not in progress.
     *
     * @throws RequestFailedException
     */
    fun getSyncing(): Request<Syncing>

    /**
     * Get the chain id.
     *
     * Get the currently configured Starknet chain id.
     *
     * @throws RequestFailedException
     */
    fun getChainId(): Request<StarknetChainId>

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockTag: BlockTag): Request<BlockWithTransactions>

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockHash: Felt): Request<BlockWithTransactions>

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockNumber: Int): Request<BlockWithTransactions>

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockTag: BlockTag): Request<BlockWithTransactionHashes>

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockHash: Felt): Request<BlockWithTransactionHashes>

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockNumber: Int): Request<BlockWithTransactionHashes>

    /**
     * Get a block with transaction receipts.
     *
     * Get block information with transaction receipts given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithReceipts(blockTag: BlockTag): Request<BlockWithReceipts>

    /**
     * Get a block with transaction receipts.
     *
     * Get block information with transaction receipts given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithReceipts(blockHash: Felt): Request<BlockWithReceipts>

    /**
     * Get a block with transaction receipts.
     *
     * Get block information with transaction receipts given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithReceipts(blockNumber: Int): Request<BlockWithReceipts>

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockTag: BlockTag): Request<StateUpdate>

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockHash: Felt): Request<StateUpdate>

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockNumber: Int): Request<StateUpdate>

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockTag: BlockTag, index: Int): Request<Transaction>

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockHash: Felt, index: Int): Request<Transaction>

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockNumber: Int, index: Int): Request<Transaction>

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockTag tag of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation
     * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockTag: BlockTag, simulationFlags: Set<SimulationFlag>): Request<SimulatedTransactionList>

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockNumber number of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation
     * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockNumber: Int, simulationFlags: Set<SimulationFlag>): Request<SimulatedTransactionList>

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockHash hash of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation
     * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockHash: Felt, simulationFlags: Set<SimulationFlag>): Request<SimulatedTransactionList>
}
