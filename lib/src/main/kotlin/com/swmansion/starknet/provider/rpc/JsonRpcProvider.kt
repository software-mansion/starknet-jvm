package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.serializers.*
import com.swmansion.starknet.data.serializers.BlockWithTransactionsPolymorphicSerializer
import com.swmansion.starknet.data.serializers.SyncPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionReceiptPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.service.http.*
import com.swmansion.starknet.service.http.requests.HttpBatchRequest
import com.swmansion.starknet.service.http.requests.HttpRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*

/**
 * A provider for interacting with Starknet using JSON-RPC. You should reuse it in your application to share the
 * httpService or provide it with your own httpService.
 *
 * @param url url of the service providing a rpc interface
 * @param httpService service used for making http requests
 */
class JsonRpcProvider(
    val url: String,
    private val httpService: HttpService,
    ignoreUnknownJsonKeys: Boolean,
) : Provider {
    private val deserializationJson = if (ignoreUnknownJsonKeys) jsonWithIgnoreUnknownKeys else Json

    constructor(url: String, ignoreUnknownJsonKeys: Boolean) : this(url, OkHttpService(), ignoreUnknownJsonKeys)
    constructor(url: String, httpService: HttpService) : this(url, httpService, false)
    constructor(url: String) : this(url, OkHttpService(), false)

    companion object {
        private val jsonWithDefaults = Json { encodeDefaults = true }
        private val jsonWithIgnoreUnknownKeys by lazy { Json { ignoreUnknownKeys = true } }

        private val defaultFeeEstimateSimulationFlags: Set<SimulationFlagForEstimateFee> by lazy {
            emptySet()
        }
    }

    private fun <T : StarknetResponse> buildRequest(
        method: JsonRpcMethod,
        paramsJson: JsonElement,
        responseSerializer: KSerializer<T>,
    ): HttpRequest<T> {
        val jsonRpcRequest = JsonRpcRequest(
            id = 0,
            jsonRpc = "2.0",
            method = method.methodName,
            params = paramsJson,
        )

        return HttpRequest(url, jsonRpcRequest, responseSerializer, deserializationJson, httpService)
    }

    /** Batch multiple various calls into a single RPC request
     *
     * @param requests list of requests to be batched together
     *
     * @return batch request
     */
    fun batchRequestsAny(requests: List<HttpRequest<out StarknetResponse>>): HttpBatchRequest<StarknetResponse> {
        require(requests.isNotEmpty()) { "Cannot create a batch request from an empty list of requests." }

        val orderedRequests = requests.mapIndexed { index, request ->
            JsonRpcRequest(
                id = index,
                jsonRpc = "2.0",
                method = request.jsonRpcRequest.method,
                params = request.jsonRpcRequest.params,
            )
        }
        val responseSerializers = requests.map { it.serializer }
        return HttpBatchRequest.fromRequestsAny(url, orderedRequests, responseSerializers, deserializationJson, httpService)
    }

    /** Batch multiple various calls into a single RPC request
     *
     * @param requests requests to be batched together
     *
     * @return batch request
     */
    fun batchRequestsAny(vararg requests: HttpRequest<out StarknetResponse>): HttpBatchRequest<StarknetResponse> {
        return batchRequestsAny(requests.toList())
    }

    /** Batch multiple calls into a single RPC request
     *
     * @param requests list of requests to be batched together
     *
     * @return batch request
     */
    fun <T : StarknetResponse> batchRequests(requests: List<HttpRequest<T>>): HttpBatchRequest<T> {
        require(requests.isNotEmpty()) { "Cannot create a batch request from an empty list of requests." }

        val orderedRequests = requests.mapIndexed { index, request ->
            JsonRpcRequest(
                id = index,
                jsonRpc = "2.0",
                method = request.jsonRpcRequest.method,
                params = request.jsonRpcRequest.params,
            )
        }
        val responseSerializers = requests.map { it.serializer }
        return HttpBatchRequest.fromRequests(url, orderedRequests, responseSerializers, deserializationJson, httpService)
    }

    /** Batch multiple calls into a single RPC request
     *
     * @param requests requests to be batched together
     *
     * @return batch request
     */
    fun <T : StarknetResponse> batchRequests(vararg requests: HttpRequest<T>): HttpBatchRequest<T> {
        return batchRequests(requests.toList())
    }

    override fun getSpecVersion(): HttpRequest<StringResponse> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(JsonRpcMethod.GET_SPEC_VERSION, params, StringResponseSerializer)
    }

    private fun callContract(payload: CallContractPayload): HttpRequest<FeltArray> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.CALL, params, FeltArraySerializer)
    }

    override fun callContract(call: Call, blockTag: BlockTag): HttpRequest<FeltArray> {
        val payload = CallContractPayload(call, BlockId.Tag(blockTag))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockHash: Felt): HttpRequest<FeltArray> {
        val payload = CallContractPayload(call, BlockId.Hash(blockHash))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockNumber: Int): HttpRequest<FeltArray> {
        val payload = CallContractPayload(call, BlockId.Number(blockNumber))

        return callContract(payload)
    }

    override fun callContract(call: Call): HttpRequest<FeltArray> {
        return callContract(call, BlockTag.LATEST)
    }

    override fun deployAccount(payload: DeployAccountTransactionV1): HttpRequest<DeployAccountResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("deploy_account_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DEPLOY_ACCOUNT_TRANSACTION, jsonPayload, DeployAccountResponse.serializer())
    }

    override fun deployAccount(payload: DeployAccountTransactionV3): HttpRequest<DeployAccountResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("deploy_account_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DEPLOY_ACCOUNT_TRANSACTION, jsonPayload, DeployAccountResponse.serializer())
    }

    private fun getStorageAt(payload: GetStorageAtPayload): HttpRequest<Felt> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STORAGE_AT, params, Felt.serializer())
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): HttpRequest<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockId.Tag(blockTag))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): HttpRequest<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockId.Hash(blockHash))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockNumber: Int): HttpRequest<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockId.Number(blockNumber))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt): HttpRequest<Felt> {
        return getStorageAt(contractAddress, key, BlockTag.LATEST)
    }

    override fun getStorageAt(contractAddress: Felt, key: String, blockTag: BlockTag): Request<Felt> {
        return getStorageAt(contractAddress, selectorFromName(key), blockTag)
    }

    override fun getStorageAt(contractAddress: Felt, key: String, blockHash: Felt): Request<Felt> {
        return getStorageAt(contractAddress, selectorFromName(key), blockHash)
    }

    override fun getStorageAt(contractAddress: Felt, key: String, blockNumber: Int): Request<Felt> {
        return getStorageAt(contractAddress, selectorFromName(key), blockNumber)
    }

    override fun getStorageAt(contractAddress: Felt, key: String): Request<Felt> {
        return getStorageAt(contractAddress, selectorFromName(key))
    }

    override fun getTransaction(transactionHash: Felt): HttpRequest<Transaction> {
        val payload = GetTransactionByHashPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_HASH, params, TransactionPolymorphicSerializer)
    }

    override fun getTransactionReceipt(transactionHash: Felt): HttpRequest<out TransactionReceipt> {
        val payload = GetTransactionReceiptPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(
            JsonRpcMethod.GET_TRANSACTION_RECEIPT,
            params,
            TransactionReceiptPolymorphicSerializer,
        )
    }

    override fun getTransactionStatus(transactionHash: Felt): HttpRequest<GetTransactionStatusResponse> {
        val payload = GetTransactionStatusPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_STATUS, params, GetTransactionStatusResponse.serializer())
    }

    override fun invokeFunction(
        payload: InvokeTransactionV1,
    ): HttpRequest<InvokeFunctionResponse> {
//        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        println("V1 $params")

        val jsonPayload = buildJsonObject {
            put("invoke_transaction", params)
        }

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, jsonPayload, InvokeFunctionResponse.serializer())
    }

    override fun invokeFunction(
        payload: InvokeTransactionV3,
    ): HttpRequest<InvokeFunctionResponse> {
//        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("invoke_transaction", params)
        }

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, jsonPayload, InvokeFunctionResponse.serializer())
    }

    private fun getClass(payload: GetClassPayload): HttpRequest<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS, params, ContractClassPolymorphicSerializer)
    }

    override fun getClass(classHash: Felt): HttpRequest<ContractClassBase> {
        return getClass(classHash, BlockTag.LATEST)
    }

    override fun getClass(classHash: Felt, blockHash: Felt): HttpRequest<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Hash(blockHash))

        return getClass(payload)
    }

    override fun getClass(classHash: Felt, blockNumber: Int): HttpRequest<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Number(blockNumber))

        return getClass(payload)
    }

    override fun getClass(classHash: Felt, blockTag: BlockTag): HttpRequest<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Tag(blockTag))

        return getClass(payload)
    }

    private fun getClassAt(payload: GetClassAtPayload): HttpRequest<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_AT, params, ContractClassPolymorphicSerializer)
    }

    override fun getClassAt(contractAddress: Felt, blockHash: Felt): HttpRequest<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Hash(blockHash), contractAddress)

        return getClassAt(payload)
    }

    override fun getClassAt(contractAddress: Felt, blockNumber: Int): HttpRequest<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Number(blockNumber), contractAddress)

        return getClassAt(payload)
    }

    override fun getClassAt(contractAddress: Felt, blockTag: BlockTag): HttpRequest<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Tag(blockTag), contractAddress)

        return getClassAt(payload)
    }

    override fun getClassAt(contractAddress: Felt): HttpRequest<ContractClassBase> {
        return getClassAt(contractAddress, BlockTag.LATEST)
    }

    private fun getClassHashAt(payload: GetClassAtPayload): HttpRequest<Felt> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_HASH_AT, params, Felt.serializer())
    }

    override fun getClassHashAt(contractAddress: Felt, blockHash: Felt): HttpRequest<Felt> {
        val payload = GetClassAtPayload(BlockId.Hash(blockHash), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt, blockNumber: Int): HttpRequest<Felt> {
        val payload = GetClassAtPayload(BlockId.Number(blockNumber), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): HttpRequest<Felt> {
        val payload = GetClassAtPayload(BlockId.Tag(blockTag), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt): HttpRequest<Felt> {
        return getClassHashAt(contractAddress, BlockTag.LATEST)
    }

    override fun declareContract(payload: DeclareTransactionV2): HttpRequest<DeclareResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("declare_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DECLARE, jsonPayload, DeclareResponse.serializer())
    }

    override fun declareContract(payload: DeclareTransactionV3): HttpRequest<DeclareResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("declare_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DECLARE, jsonPayload, DeclareResponse.serializer())
    }

    override fun getBlockNumber(): HttpRequest<IntResponse> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_NUMBER,
            params,
            IntResponseSerializer,
        )
    }

    override fun getBlockHashAndNumber(): HttpRequest<GetBlockHashAndNumberResponse> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_HASH_AND_NUMBER,
            params,
            GetBlockHashAndNumberResponse.serializer(),
        )
    }

    private fun getBlockTransactionCount(payload: GetBlockTransactionCountPayload): HttpRequest<IntResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_TRANSACTION_COUNT,
            params,
            IntResponseSerializer,
        )
    }

    override fun getBlockTransactionCount(blockTag: BlockTag): HttpRequest<IntResponse> {
        val payload = GetBlockTransactionCountPayload(BlockId.Tag(blockTag))

        return getBlockTransactionCount(payload)
    }

    override fun getBlockTransactionCount(blockHash: Felt): HttpRequest<IntResponse> {
        val payload = GetBlockTransactionCountPayload(BlockId.Hash(blockHash))

        return getBlockTransactionCount(payload)
    }

    override fun getBlockTransactionCount(blockNumber: Int): HttpRequest<IntResponse> {
        val payload = GetBlockTransactionCountPayload(BlockId.Number(blockNumber))

        return getBlockTransactionCount(payload)
    }

    override fun getEvents(payload: GetEventsPayload): HttpRequest<GetEventsResult> {
        val params = Json.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("filter", params)
        }

        return buildRequest(JsonRpcMethod.GET_EVENTS, jsonPayload, GetEventsResult.serializer())
    }

    private fun getEstimateFee(payload: EstimateTransactionFeePayload): HttpRequest<EstimateFeeResponseList> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_FEE, jsonPayload, EstimateFeeResponseListSerializer)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        blockHash: Felt,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Hash(blockHash))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        blockHash: Felt,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, blockHash, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        blockNumber: Int,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Number(blockNumber))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        blockNumber: Int,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, blockNumber, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Tag(blockTag))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        blockTag: BlockTag,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, blockTag, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(
        payload: List<Transaction>,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, BlockTag.PENDING, simulationFlags)
    }

    override fun getEstimateFee(payload: List<Transaction>): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, BlockTag.PENDING, defaultFeeEstimateSimulationFlags)
    }

    private fun getEstimateMessageFee(payload: EstimateMessageFeePayload): HttpRequest<EstimateFeeResponse> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_MESSAGE_FEE, jsonPayload, EstimateFeeResponse.serializer())
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockHash: Felt): HttpRequest<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Hash(blockHash))

        return getEstimateMessageFee(estimatePayload)
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockNumber: Int): HttpRequest<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Number(blockNumber))

        return getEstimateMessageFee(estimatePayload)
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockTag: BlockTag): HttpRequest<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Tag(blockTag))

        return getEstimateMessageFee(estimatePayload)
    }

    private fun getNonce(payload: GetNoncePayload): HttpRequest<Felt> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_NONCE, jsonPayload, Felt.serializer())
    }

    override fun getNonce(contractAddress: Felt): HttpRequest<Felt> =
        getNonce(contractAddress, blockTag = BlockTag.PENDING)

    override fun getNonce(contractAddress: Felt, blockTag: BlockTag): HttpRequest<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Tag(blockTag))

        return getNonce(payload)
    }

    override fun getNonce(contractAddress: Felt, blockNumber: Int): HttpRequest<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Number(blockNumber))

        return getNonce(payload)
    }

    override fun getNonce(contractAddress: Felt, blockHash: Felt): HttpRequest<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Hash(blockHash))

        return getNonce(payload)
    }

    override fun getSyncing(): HttpRequest<Syncing> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_SYNCING,
            params,
            SyncPolymorphicSerializer,
        )
    }

    override fun getChainId(): HttpRequest<StarknetChainId> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(JsonRpcMethod.GET_CHAIN_ID, params, StarknetChainId.serializer())
    }

    private fun getBlockWithTxs(payload: GetBlockWithTransactionsPayload): HttpRequest<BlockWithTransactions> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TXS, jsonPayload, BlockWithTransactionsPolymorphicSerializer)
    }

    override fun getBlockWithTxs(blockTag: BlockTag): HttpRequest<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Tag(blockTag))

        return getBlockWithTxs(payload)
    }

    override fun getBlockWithTxs(blockHash: Felt): HttpRequest<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Hash(blockHash))

        return getBlockWithTxs(payload)
    }

    override fun getBlockWithTxs(blockNumber: Int): HttpRequest<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Number(blockNumber))

        return getBlockWithTxs(payload)
    }

    override fun getBlockWithTxHashes(blockTag: BlockTag): HttpRequest<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Tag(blockTag))

        return getBlockWithTxHashes(payload)
    }

    override fun getBlockWithTxHashes(blockHash: Felt): HttpRequest<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Hash(blockHash))

        return getBlockWithTxHashes(payload)
    }

    override fun getBlockWithTxHashes(blockNumber: Int): HttpRequest<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Number(blockNumber))

        return getBlockWithTxHashes(payload)
    }

    private fun getBlockWithTxHashes(payload: GetBlockWithTransactionHashesPayload): HttpRequest<BlockWithTransactionHashes> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TX_HASHES, jsonPayload, BlockWithTransactionHashesPolymorphicSerializer)
    }

    override fun getBlockWithReceipts(blockTag: BlockTag): HttpRequest<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Tag(blockTag))

        return getBlockWithReceipts(payload)
    }

    override fun getBlockWithReceipts(blockHash: Felt): HttpRequest<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Hash(blockHash))

        return getBlockWithReceipts(payload)
    }

    override fun getBlockWithReceipts(blockNumber: Int): HttpRequest<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Number(blockNumber))

        return getBlockWithReceipts(payload)
    }

    private fun getBlockWithReceipts(payload: GetBlockWithReceiptsPayload): HttpRequest<BlockWithReceipts> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_RECEIPTS, jsonPayload, BlockWithReceiptsPolymorphicSerializer)
    }

    private fun getStateUpdate(payload: GetStateUpdatePayload): HttpRequest<StateUpdate> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STATE_UPDATE, jsonPayload, StateUpdatePolymorphicSerializer)
    }

    override fun getStateUpdate(blockTag: BlockTag): HttpRequest<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Tag(blockTag))

        return getStateUpdate(payload)
    }

    override fun getStateUpdate(blockHash: Felt): HttpRequest<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Hash(blockHash))

        return getStateUpdate(payload)
    }

    override fun getStateUpdate(blockNumber: Int): HttpRequest<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Number(blockNumber))

        return getStateUpdate(payload)
    }

    private fun getTransactionByBlockIdAndIndex(payload: GetTransactionByBlockIdAndIndexPayload): HttpRequest<Transaction> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_BLOCK_ID_AND_INDEX, jsonPayload, TransactionPolymorphicSerializer)
    }

    override fun getTransactionByBlockIdAndIndex(blockTag: BlockTag, index: Int): HttpRequest<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Tag(blockTag), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    override fun getTransactionByBlockIdAndIndex(blockHash: Felt, index: Int): HttpRequest<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Hash(blockHash), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    override fun getTransactionByBlockIdAndIndex(blockNumber: Int, index: Int): HttpRequest<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Number(blockNumber), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    private fun simulateTransactions(payload: SimulateTransactionsPayload): HttpRequest<SimulatedTransactionList> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.SIMULATE_TRANSACTIONS, params, SimulatedTransactionListSerializer)
    }

    override fun simulateTransactions(
        transactions: List<Transaction>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlag>,
    ): HttpRequest<SimulatedTransactionList> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Tag(blockTag), simulationFlags)

        return simulateTransactions(payload)
    }

    override fun simulateTransactions(
        transactions: List<Transaction>,
        blockNumber: Int,
        simulationFlags: Set<SimulationFlag>,
    ): HttpRequest<SimulatedTransactionList> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Number(blockNumber), simulationFlags)

        return simulateTransactions(payload)
    }

    override fun simulateTransactions(
        transactions: List<Transaction>,
        blockHash: Felt,
        simulationFlags: Set<SimulationFlag>,
    ): HttpRequest<SimulatedTransactionList> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Hash(blockHash), simulationFlags)

        return simulateTransactions(payload)
    }
}

private enum class JsonRpcMethod(val methodName: String) {
    GET_SPEC_VERSION("starknet_specVersion"),
    CALL("starknet_call"),
    INVOKE_TRANSACTION("starknet_addInvokeTransaction"),
    GET_STORAGE_AT("starknet_getStorageAt"),
    GET_CLASS("starknet_getClass"),
    GET_CLASS_AT("starknet_getClassAt"),
    GET_CLASS_HASH_AT("starknet_getClassHashAt"),
    GET_TRANSACTION_BY_HASH("starknet_getTransactionByHash"),
    GET_TRANSACTION_RECEIPT("starknet_getTransactionReceipt"),
    GET_TRANSACTION_STATUS("starknet_getTransactionStatus"),
    DECLARE("starknet_addDeclareTransaction"),
    GET_EVENTS("starknet_getEvents"),
    GET_BLOCK_NUMBER("starknet_blockNumber"),
    GET_BLOCK_HASH_AND_NUMBER("starknet_blockHashAndNumber"),
    GET_BLOCK_TRANSACTION_COUNT("starknet_getBlockTransactionCount"),
    GET_SYNCING("starknet_syncing"),
    GET_CHAIN_ID("starknet_chainId"),
    ESTIMATE_FEE("starknet_estimateFee"),
    ESTIMATE_MESSAGE_FEE("starknet_estimateMessageFee"),
    GET_BLOCK_WITH_TXS("starknet_getBlockWithTxs"),
    GET_BLOCK_WITH_TX_HASHES("starknet_getBlockWithTxHashes"),
    GET_BLOCK_WITH_RECEIPTS("starknet_getBlockWithReceipts"),
    GET_STATE_UPDATE("starknet_getStateUpdate"),
    GET_TRANSACTION_BY_BLOCK_ID_AND_INDEX("starknet_getTransactionByBlockIdAndIndex"),
    GET_NONCE("starknet_getNonce"),
    DEPLOY_ACCOUNT_TRANSACTION("starknet_addDeployAccountTransaction"),
    SIMULATE_TRANSACTIONS("starknet_simulateTransactions"),
}
