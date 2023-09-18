package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.*
import com.swmansion.starknet.data.serializers.JsonRpcGetBlockWithTransactionsPolymorphicSerializer
import com.swmansion.starknet.data.serializers.JsonRpcSyncPolymorphicSerializer
import com.swmansion.starknet.data.serializers.JsonRpcTransactionReceiptPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpRequest
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkHttpService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

/**
 * A provider for interacting with Starknet using JSON-RPC. You should reuse it in your application to share the
 * httpService or provide it with your own httpService.
 *
 * @param url url of the service providing a rpc interface
 * @param chainId an id of the network
 * @param httpService service used for making http requests
 */
class JsonRpcProvider(
    val url: String,
    override val chainId: StarknetChainId,
    private val httpService: HttpService,
) : Provider {
    constructor(url: String, chainId: StarknetChainId) : this(url, chainId, OkHttpService())

    private val jsonWithDefaults = Json { encodeDefaults = true }

    private fun buildRequestJson(method: String, paramsJson: JsonElement): Map<String, JsonElement> {
        val map = mapOf(
            "jsonrpc" to JsonPrimitive("2.0"),
            "method" to JsonPrimitive(method),
            "id" to JsonPrimitive(0), // It is not used anywhere
            "params" to paramsJson,
        )

        return JsonObject(map)
    }

    private fun <T> buildRequest(
        method: JsonRpcMethod,
        paramsJson: JsonElement,
        responseSerializer: KSerializer<T>,
    ): HttpRequest<T> {
        val requestJson = buildRequestJson(method.methodName, paramsJson)

        val payload =
            HttpService.Payload(url, "POST", emptyList(), requestJson.toString())

        return HttpRequest(payload, buildJsonHttpDeserializer(responseSerializer), httpService)
    }

    private fun callContract(payload: CallContractPayload): Request<List<Felt>> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.CALL, params, ListSerializer(Felt.serializer()))
    }

    override fun callContract(call: Call, blockTag: BlockTag): Request<List<Felt>> {
        val payload = CallContractPayload(call, BlockId.Tag(blockTag))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockHash: Felt): Request<List<Felt>> {
        val payload = CallContractPayload(call, BlockId.Hash(blockHash))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockNumber: Int): Request<List<Felt>> {
        val payload = CallContractPayload(call, BlockId.Number(blockNumber))

        return callContract(payload)
    }

    override fun deployAccount(payload: DeployAccountTransactionPayload): Request<DeployAccountResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("deploy_account_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DEPLOY_ACCOUNT_TRANSACTION, jsonPayload, DeployAccountResponse.serializer())
    }

    private fun getStorageAt(payload: GetStorageAtPayload): Request<Felt> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STORAGE_AT, params, Felt.serializer())
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockId.Tag(blockTag))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockId.Hash(blockHash))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockNumber: Int): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockId.Number(blockNumber))

        return getStorageAt(payload)
    }

    override fun getTransaction(transactionHash: Felt): Request<Transaction> {
        val payload = GetTransactionByHashPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_HASH, params, TransactionPolymorphicSerializer)
    }

    override fun getTransactionReceipt(transactionHash: Felt): Request<out TransactionReceipt> {
        val payload = GetTransactionReceiptPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(
            JsonRpcMethod.GET_TRANSACTION_RECEIPT,
            params,
            JsonRpcTransactionReceiptPolymorphicSerializer,
        )
    }

    override fun invokeFunction(
        payload: InvokeTransactionPayload,
    ): Request<InvokeFunctionResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("invoke_transaction", params)
        }

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, jsonPayload, InvokeFunctionResponse.serializer())
    }

    private fun getClass(payload: GetClassPayload): Request<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS, params, JsonRpcContractClassPolymorphicSerializer)
    }

    override fun getClass(classHash: Felt): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockTag.LATEST.tag)

        return getClass(payload)
    }

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
    fun getClass(classHash: Felt, blockHash: Felt): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, blockHash.hexString())

        return getClass(payload)
    }

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
    fun getClass(classHash: Felt, blockNumber: Int): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, blockNumber.toString())

        return getClass(payload)
    }

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
    fun getClass(classHash: Felt, blockTag: BlockTag): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, blockTag.tag)

        return getClass(payload)
    }

    private fun getClassAt(payload: GetClassAtPayload): Request<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_AT, params, JsonRpcContractClassPolymorphicSerializer)
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockHash The hash of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockHash: Felt): Request<ContractClassBase> {
        val payload = GetClassAtPayload(blockHash.hexString(), contractAddress)

        return getClassAt(payload)
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockNumber The number of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockNumber: Int): Request<ContractClassBase> {
        val payload = GetClassAtPayload(blockNumber.toString(), contractAddress)

        return getClassAt(payload)
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition at the given address in the given block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     * @param blockTag The tag of the requested block.
     */
    fun getClassAt(contractAddress: Felt, blockTag: BlockTag): Request<ContractClassBase> {
        val payload = GetClassAtPayload(blockTag.tag, contractAddress)

        return getClassAt(payload)
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition in the at the given address in the latest block.
     *
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassAt(contractAddress: Felt): Request<ContractClassBase> {
        return getClassAt(contractAddress, BlockTag.LATEST)
    }

    private fun getClassHashAt(payload: GetClassAtPayload): Request<Felt> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_HASH_AT, params, Felt.serializer())
    }

    override fun getClassHashAt(contractAddress: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetClassAtPayload(blockHash.hexString(), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt, blockNumber: Int): Request<Felt> {
        val payload = GetClassAtPayload(blockNumber.toString(), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetClassAtPayload(blockTag.tag, contractAddress)

        return getClassHashAt(payload)
    }

    override fun declareContract(payload: DeclareTransactionV1Payload): Request<DeclareResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(DeclareTransactionV1PayloadSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("declare_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DECLARE, jsonPayload, DeclareResponse.serializer())
    }

    override fun declareContract(payload: DeclareTransactionV2Payload): Request<DeclareResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(DeclareTransactionV2PayloadSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("declare_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DECLARE, jsonPayload, DeclareResponse.serializer())
    }

    override fun getBlockNumber(): Request<Int> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_NUMBER,
            params,
            Int.serializer(),
        )
    }

    override fun getBlockHashAndNumber(): Request<GetBlockHashAndNumberResponse> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_HASH_AND_NUMBER,
            params,
            GetBlockHashAndNumberResponse.serializer(),
        )
    }

    private fun getBlockTransactionCount(payload: GetBlockTransactionCountPayload): Request<Int> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_TRANSACTION_COUNT,
            params,
            Int.serializer(),
        )
    }

    override fun getBlockTransactionCount(blockTag: BlockTag): Request<Int> {
        val payload = GetBlockTransactionCountPayload(BlockId.Tag(blockTag))

        return getBlockTransactionCount(payload)
    }

    override fun getBlockTransactionCount(blockHash: Felt): Request<Int> {
        val payload = GetBlockTransactionCountPayload(BlockId.Hash(blockHash))

        return getBlockTransactionCount(payload)
    }

    override fun getBlockTransactionCount(blockNumber: Int): Request<Int> {
        val payload = GetBlockTransactionCountPayload(BlockId.Number(blockNumber))

        return getBlockTransactionCount(payload)
    }

    /**
     * Get events
     *
     * Returns all events matching the given filter
     *
     * @param payload get events payload
     * @return GetEventsResult with list of emitted events and additional page info
     *
     * @throws RpcRequestFailedException
     */
    fun getEvents(payload: GetEventsPayload): Request<GetEventsResult> {
        val params = Json.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("filter", params)
        }

        return buildRequest(JsonRpcMethod.GET_EVENTS, jsonPayload, GetEventsResult.serializer())
    }

    private fun getEstimateFee(payload: EstimateTransactionFeePayload): Request<List<EstimateFeeResponse>> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(EstimateTransactionFeePayloadSerializer, payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_FEE, jsonPayload, ListSerializer(EstimateFeeResponse.serializer()))
    }

    override fun getEstimateFee(payload: List<TransactionPayload>, blockHash: Felt): Request<List<EstimateFeeResponse>> {
        val estimatePayload = EstimateTransactionFeePayload(payload, BlockId.Hash(blockHash))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(payload: List<TransactionPayload>, blockNumber: Int): Request<List<EstimateFeeResponse>> {
        val estimatePayload = EstimateTransactionFeePayload(payload, BlockId.Number(blockNumber))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(payload: List<TransactionPayload>, blockTag: BlockTag): Request<List<EstimateFeeResponse>> {
        val estimatePayload = EstimateTransactionFeePayload(payload, BlockId.Tag(blockTag))

        return getEstimateFee(estimatePayload)
    }
    private fun getEstimateMessageFee(payload: EstimateMessageFeePayload): Request<EstimateFeeResponse> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_MESSAGE_FEE, jsonPayload, EstimateFeeResponse.serializer())
    }

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
    fun getEstimateMessageFee(message: MessageL1ToL2, blockHash: Felt): Request<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Hash(blockHash))

        return getEstimateMessageFee(estimatePayload)
    }

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
    fun getEstimateMessageFee(message: MessageL1ToL2, blockNumber: Int): Request<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Number(blockNumber))

        return getEstimateMessageFee(estimatePayload)
    }

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
    fun getEstimateMessageFee(message: MessageL1ToL2, blockTag: BlockTag): Request<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Tag(blockTag))

        return getEstimateMessageFee(estimatePayload)
    }

    private fun getNonce(payload: GetNoncePayload): Request<Felt> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_NONCE, jsonPayload, Felt.serializer())
    }

    override fun getNonce(contractAddress: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Tag(blockTag))

        return getNonce(payload)
    }

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
    fun getNonce(contractAddress: Felt, blockNumber: Int): Request<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Number(blockNumber))

        return getNonce(payload)
    }

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
    fun getNonce(contractAddress: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Hash(blockHash))

        return getNonce(payload)
    }

    /**
     * Get the block synchronization status.
     *
     * Get the starting, current and highest block info or boolean indicating syncing is not in progress.
     *
     * @throws RequestFailedException
     */
    fun getSyncing(): Request<Syncing> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_SYNCING,
            params,
            JsonRpcSyncPolymorphicSerializer,
        )
    }

    private fun getBlockWithTxs(payload: GetBlockWithTransactionsPayload): Request<GetBlockWithTransactionsResponse> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TXS, jsonPayload, JsonRpcGetBlockWithTransactionsPolymorphicSerializer)
    }

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockTag: BlockTag): Request<GetBlockWithTransactionsResponse> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Tag(blockTag))

        return getBlockWithTxs(payload)
    }

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockHash: Felt): Request<GetBlockWithTransactionsResponse> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Hash(blockHash))

        return getBlockWithTxs(payload)
    }

    /**
     * Get a block with transactions.
     *
     * Get block information with full transactions given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxs(blockNumber: Int): Request<GetBlockWithTransactionsResponse> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Number(blockNumber))

        return getBlockWithTxs(payload)
    }

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockTag: BlockTag): Request<GetBlockWithTransactionHashesResponse> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Tag(blockTag))

        return getBlockWithTxHashes(payload)
    }

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockHash: Felt): Request<GetBlockWithTransactionHashesResponse> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Hash(blockHash))

        return getBlockWithTxHashes(payload)
    }

    /**
     * Get a block with transaction hashes.
     *
     * Get block information with transaction hashes given the block id.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getBlockWithTxHashes(blockNumber: Int): Request<GetBlockWithTransactionHashesResponse> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Number(blockNumber))

        return getBlockWithTxHashes(payload)
    }

    private fun getBlockWithTxHashes(payload: GetBlockWithTransactionHashesPayload): Request<GetBlockWithTransactionHashesResponse> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TX_HASHES, jsonPayload, JsonRpcGetBlockWithTransactionHashesPolymorphicSerializer)
    }

    private fun getStateUpdate(payload: GetStateUpdatePayload): Request<StateUpdate> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STATE_UPDATE, jsonPayload, JsonRpcStateUpdatePolymorphicSerializer)
    }

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockTag: BlockTag): Request<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Tag(blockTag))

        return getStateUpdate(payload)
    }

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockHash: Felt): Request<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Hash(blockHash))

        return getStateUpdate(payload)
    }

    /**
     * Get block state information.
     *
     * Get the information about the result of executing the requested block.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getStateUpdate(blockNumber: Int): Request<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Number(blockNumber))

        return getStateUpdate(payload)
    }

    private fun getTransactionByBlockIdAndIndex(payload: GetTransactionByBlockIdAndIndexPayload): Request<Transaction> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_BLOCK_ID_AND_INDEX, jsonPayload, Transaction.serializer())
    }

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockTag a tag of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockTag: BlockTag, index: Int): Request<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Tag(blockTag), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockHash a hash of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockHash: Felt, index: Int): Request<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Hash(blockHash), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    /**
     * Get transaction by block id and index.
     *
     * Get the details of a transaction by a given block id and index.
     *
     * @param blockNumber a number of the requested block
     *
     * @throws RequestFailedException
     */
    fun getTransactionByBlockIdAndIndex(blockNumber: Int, index: Int): Request<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Number(blockNumber), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    /**
     * Get pending transactions.
     *
     * Returns the transactions in the transaction pool, recognized by this sequencer.
     *
     * @throws RequestFailedException
     */
    fun getPendingTransactions(): Request<List<Transaction>> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_PENDING_TRANSACTIONS,
            params,
            ListSerializer(TransactionPolymorphicSerializer),
        )
    }

    private fun simulateTransactions(payload: SimulateTransactionsPayload): Request<List<SimulatedTransaction>> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.SIMULATE_TRANSACTIONS, params, ListSerializer(SimulatedTransaction.serializer()))
    }

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockTag tag of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockTag: BlockTag, simulationFlags: Set<SimulationFlag>): Request<List<SimulatedTransaction>> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Tag(blockTag), simulationFlags)

        return simulateTransactions(payload)
    }

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockNumber number of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockNumber: Int, simulationFlags: Set<SimulationFlag>): Request<List<SimulatedTransaction>> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Number(blockNumber), simulationFlags)

        return simulateTransactions(payload)
    }

    /** Simulate executing a list of transactions
     *
     * @param transactions list of transactions to be simulated
     * @param blockHash hash of the block that should be used for simulation
     * @param simulationFlags set of flags to be used for simulation * @return a list of transaction simulations
     */
    fun simulateTransactions(transactions: List<TransactionPayload>, blockHash: Felt, simulationFlags: Set<SimulationFlag>): Request<List<SimulatedTransaction>> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Hash(blockHash), simulationFlags)

        return simulateTransactions(payload)
    }
}

private enum class JsonRpcMethod(val methodName: String) {
    CALL("starknet_call"),
    INVOKE_TRANSACTION("starknet_addInvokeTransaction"),
    GET_STORAGE_AT("starknet_getStorageAt"),
    GET_CLASS("starknet_getClass"),
    GET_CLASS_AT("starknet_getClassAt"),
    GET_CLASS_HASH_AT("starknet_getClassHashAt"),
    GET_TRANSACTION_BY_HASH("starknet_getTransactionByHash"),
    GET_TRANSACTION_RECEIPT("starknet_getTransactionReceipt"),
    DECLARE("starknet_addDeclareTransaction"),
    GET_EVENTS("starknet_getEvents"),
    GET_BLOCK_NUMBER("starknet_blockNumber"),
    GET_BLOCK_HASH_AND_NUMBER("starknet_blockHashAndNumber"),
    GET_BLOCK_TRANSACTION_COUNT("starknet_getBlockTransactionCount"),
    GET_SYNCING("starknet_syncing"),
    ESTIMATE_FEE("starknet_estimateFee"),
    ESTIMATE_MESSAGE_FEE("starknet_estimateMessageFee"),
    GET_BLOCK_WITH_TXS("starknet_getBlockWithTxs"),
    GET_BLOCK_WITH_TX_HASHES("starknet_getBlockWithTxHashes"),
    GET_STATE_UPDATE("starknet_getStateUpdate"),
    GET_TRANSACTION_BY_BLOCK_ID_AND_INDEX("starknet_getTransactionByBlockIdAndIndex"),
    GET_PENDING_TRANSACTIONS("starknet_pendingTransactions"),
    GET_NONCE("starknet_getNonce"),
    DEPLOY_ACCOUNT_TRANSACTION("starknet_addDeployAccountTransaction"),
    SIMULATE_TRANSACTIONS("starknet_simulateTransactions"),
}
