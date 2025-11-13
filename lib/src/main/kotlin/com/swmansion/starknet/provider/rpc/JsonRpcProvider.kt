package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.serializers.*
import com.swmansion.starknet.data.serializers.BlockWithTransactionsPolymorphicSerializer
import com.swmansion.starknet.data.serializers.SyncPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionReceiptPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.MessageStatusList
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

    init {
        println("Using JsonRpcProvider with url: $url")
        println("url.length=${url.length}")
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
     *
     * @sample starknet.provider.ProviderTest.batchRequestsAny
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
     *
     * @sample starknet.provider.ProviderTest.batchGetTransactions
     */
    fun <T : StarknetResponse> batchRequests(vararg requests: HttpRequest<T>): HttpBatchRequest<T> {
        return batchRequests(requests.toList())
    }

    /**
     * @sample starknet.provider.ProviderTest.getSpecVersion
     */
    override fun getSpecVersion(): HttpRequest<StringResponse> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(JsonRpcMethod.GET_SPEC_VERSION, params, StringResponseSerializer)
    }

    private fun callContract(payload: CallContractPayload): HttpRequest<FeltArray> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.CALL, params, FeltArraySerializer)
    }

    /**
     * @sample starknet.provider.ProviderTest.callContractWithBlockTag
     */
    override fun callContract(call: Call, blockTag: BlockTag): HttpRequest<FeltArray> {
        val payload = CallContractPayload(call, BlockId.Tag(blockTag))

        return callContract(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.callContractWithBlockHash
     */
    override fun callContract(call: Call, blockHash: Felt): HttpRequest<FeltArray> {
        val payload = CallContractPayload(call, BlockId.Hash(blockHash))

        return callContract(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.callContractWithBlockNumber
     */
    override fun callContract(call: Call, blockNumber: Int): HttpRequest<FeltArray> {
        val payload = CallContractPayload(call, BlockId.Number(blockNumber))

        return callContract(payload)
    }

    override fun callContract(call: Call): HttpRequest<FeltArray> {
        return callContract(call, BlockTag.LATEST)
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

    /**
     * @sample starknet.provider.ProviderTest.getStorageAt
     */
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

    /**
     * @sample starknet.provider.ProviderTest.getStorageAtWithKeyAsString
     */
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

    /**
     * @sample starknet.provider.ProviderTest.getInvokeTransaction
     */
    override fun getTransaction(transactionHash: Felt): HttpRequest<Transaction> {
        val payload = GetTransactionByHashPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_HASH, params, TransactionSerializer)
    }

    /**
     * @sample starknet.provider.ProviderTest.getInvokeTransactionReceipt
     */
    override fun getTransactionReceipt(transactionHash: Felt): HttpRequest<out TransactionReceipt> {
        val payload = GetTransactionReceiptPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(
            JsonRpcMethod.GET_TRANSACTION_RECEIPT,
            params,
            TransactionReceiptPolymorphicSerializer,
        )
    }

    /**
     * @sample starknet.provider.ProviderTest.getTransactionStatus
     */
    override fun getTransactionStatus(transactionHash: Felt): HttpRequest<GetTransactionStatusResponse> {
        val payload = GetTransactionStatusPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_STATUS, params, GetTransactionStatusResponse.serializer())
    }

    private fun getMessagesStatus(payload: GetMessagesStatusPayload): HttpRequest<MessageStatusList> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_MESSAGES_STATUS, params, MessageStatusListSerializer)
    }

    override fun getMessagesStatus(l1TransactionHash: NumAsHex): HttpRequest<MessageStatusList> {
        val payload = GetMessagesStatusPayload(l1TransactionHash)

        return getMessagesStatus(payload)
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeTest.signV3SingleCall
     */
    override fun invokeFunction(
        payload: InvokeTransactionV3,
    ): HttpRequest<InvokeFunctionResponse> {
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

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtClassHash
     */
    override fun getClass(classHash: Felt): HttpRequest<ContractClassBase> {
        return getClass(classHash, BlockTag.LATEST)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtClassHashWithBlockHash
     */
    override fun getClass(classHash: Felt, blockHash: Felt): HttpRequest<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Hash(blockHash))

        return getClass(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtClassHashWithBlockNumber
     */
    override fun getClass(classHash: Felt, blockNumber: Int): HttpRequest<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Number(blockNumber))

        return getClass(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtClassHashWithLatestBlock
     */
    override fun getClass(classHash: Felt, blockTag: BlockTag): HttpRequest<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Tag(blockTag))

        return getClass(payload)
    }

    private fun getClassAt(payload: GetClassAtPayload): HttpRequest<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_AT, params, ContractClassPolymorphicSerializer)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtContractAddressWithBlockHash
     */
    override fun getClassAt(contractAddress: Felt, blockHash: Felt): HttpRequest<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Hash(blockHash), contractAddress)

        return getClassAt(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtContractAddressWithBlockNumber
     */
    override fun getClassAt(contractAddress: Felt, blockNumber: Int): HttpRequest<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Number(blockNumber), contractAddress)

        return getClassAt(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtContractAddressWithLatestBlock
     */
    override fun getClassAt(contractAddress: Felt, blockTag: BlockTag): HttpRequest<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Tag(blockTag), contractAddress)

        return getClassAt(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassDefinitionAtContractAddress
     */
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

    /**
     * @sample starknet.provider.ProviderTest.getClassHashAtPendingBlock
     */
    override fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): HttpRequest<Felt> {
        val payload = GetClassAtPayload(BlockId.Tag(blockTag), contractAddress)

        return getClassHashAt(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getClassHash
     */
    override fun getClassHashAt(contractAddress: Felt): HttpRequest<Felt> {
        return getClassHashAt(contractAddress, BlockTag.LATEST)
    }

    override fun declareContract(payload: DeclareTransactionV3): HttpRequest<DeclareResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(TransactionSerializer, payload)
        val jsonPayload = buildJsonObject {
            put("declare_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DECLARE, jsonPayload, DeclareResponse.serializer())
    }

    /**
     * @sample starknet.provider.ProviderTest.getCurrentBlockNumber
     */
    override fun getBlockNumber(): HttpRequest<IntResponse> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_NUMBER,
            params,
            IntResponseSerializer,
        )
    }

    /**
     * @sample starknet.provider.ProviderTest.getCurrentBlockHashAndNumber
     */
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

    /**
     * @sample starknet.provider.ProviderTest.getBlockTransactionCountWithBlockTag
     */
    override fun getBlockTransactionCount(blockTag: BlockTag): HttpRequest<IntResponse> {
        val payload = GetBlockTransactionCountPayload(BlockId.Tag(blockTag))

        return getBlockTransactionCount(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockTransactionCountWithBlockHash
     */
    override fun getBlockTransactionCount(blockHash: Felt): HttpRequest<IntResponse> {
        val payload = GetBlockTransactionCountPayload(BlockId.Hash(blockHash))

        return getBlockTransactionCount(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockTransactionCountWithBlockNumber
     */
    override fun getBlockTransactionCount(blockNumber: Int): HttpRequest<IntResponse> {
        val payload = GetBlockTransactionCountPayload(BlockId.Number(blockNumber))

        return getBlockTransactionCount(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getEvents
     */
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
        payload: List<ExecutableTransaction>,
        blockHash: Felt,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Hash(blockHash))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<ExecutableTransaction>,
        blockHash: Felt,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, blockHash, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(
        payload: List<ExecutableTransaction>,
        blockNumber: Int,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Number(blockNumber))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<ExecutableTransaction>,
        blockNumber: Int,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, blockNumber, defaultFeeEstimateSimulationFlags)
    }

    /**
     * @sample network.account.AccountTest.estimateFeeForDeclareV3Transaction
     */
    override fun getEstimateFee(
        payload: List<ExecutableTransaction>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Tag(blockTag))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<ExecutableTransaction>,
        blockTag: BlockTag,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, blockTag, defaultFeeEstimateSimulationFlags)
    }

    /**
     * @sample starknet.account.StandardAccountTest.InvokeEstimateTest.estimateFeeWithSkipValidateFlag
     */
    override fun getEstimateFee(
        payload: List<ExecutableTransaction>,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, BlockTag.PRE_CONFIRMED, simulationFlags)
    }

    /**
     * @sample starknet.account.StandardAccountTest.DeployAccountEstimateTest.estimateFeeForDeployAccountV3Transaction
     */
    override fun getEstimateFee(payload: List<ExecutableTransaction>): HttpRequest<EstimateFeeResponseList> {
        return getEstimateFee(payload, BlockTag.PRE_CONFIRMED, defaultFeeEstimateSimulationFlags)
    }

    private fun getEstimateMessageFee(payload: EstimateMessageFeePayload): HttpRequest<EstimateMessageFeeResponse> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_MESSAGE_FEE, jsonPayload, EstimateMessageFeeResponse.serializer())
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockHash: Felt): HttpRequest<EstimateMessageFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Hash(blockHash))

        return getEstimateMessageFee(estimatePayload)
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockNumber: Int): HttpRequest<EstimateMessageFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Number(blockNumber))

        return getEstimateMessageFee(estimatePayload)
    }

    /**
     * @sample starknet.account.StandardAccountTest.estimateMessageFee
     */
    override fun getEstimateMessageFee(message: MessageL1ToL2, blockTag: BlockTag): HttpRequest<EstimateMessageFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Tag(blockTag))

        return getEstimateMessageFee(estimatePayload)
    }

    private fun getNonce(payload: GetNoncePayload): HttpRequest<Felt> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_NONCE, jsonPayload, Felt.serializer())
    }

    override fun getNonce(contractAddress: Felt): HttpRequest<Felt> =
        getNonce(contractAddress, blockTag = BlockTag.PRE_CONFIRMED)

    /**
     * @sample starknet.provider.ProviderTest.getNonceWithBlockTag
     */
    override fun getNonce(contractAddress: Felt, blockTag: BlockTag): HttpRequest<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Tag(blockTag))

        return getNonce(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getNonceWithBlockNumber
     */
    override fun getNonce(contractAddress: Felt, blockNumber: Int): HttpRequest<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Number(blockNumber))

        return getNonce(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getNonceWithBlockHash
     */
    override fun getNonce(contractAddress: Felt, blockHash: Felt): HttpRequest<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Hash(blockHash))

        return getNonce(payload)
    }

    private fun getStorageProof(payload: GetStorageProofPayload): HttpRequest<StorageProof> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STORAGE_PROOF, jsonPayload, StorageProof.serializer())
    }

    override fun getStorageProof(
        blockId: BlockId,
        classHashes: List<Felt>?,
        contractAddresses: List<Felt>?,
        contractsStorageKeys: List<ContractsStorageKeys>?,
    ): HttpRequest<StorageProof> {
        require(blockId != BlockId.Tag(BlockTag.PRE_CONFIRMED)) {
            "Pre-confirmed block tag is not allowed for `getStorageProof`"
        }

        val payload = GetStorageProofPayload(blockId, classHashes, contractAddresses, contractsStorageKeys)

        return getStorageProof(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getSyncInformationNodeNotSyncing
     */
    override fun getSyncing(): HttpRequest<Syncing> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_SYNCING,
            params,
            SyncPolymorphicSerializer,
        )
    }

    /**
     * @sample starknet.provider.ProviderTest.getChainId
     */
    override fun getChainId(): HttpRequest<StarknetChainId> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(JsonRpcMethod.GET_CHAIN_ID, params, StarknetChainId.serializer())
    }

    private fun getBlockWithTxs(payload: GetBlockWithTransactionsPayload): HttpRequest<BlockWithTransactions> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TXS, jsonPayload, BlockWithTransactionsPolymorphicSerializer)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionsWithBlockTag
     */
    override fun getBlockWithTxs(blockTag: BlockTag): HttpRequest<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Tag(blockTag))

        return getBlockWithTxs(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionsWithBlockHash
     */
    override fun getBlockWithTxs(blockHash: Felt): HttpRequest<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Hash(blockHash))

        return getBlockWithTxs(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionsWithBlockNumber
     */
    override fun getBlockWithTxs(blockNumber: Int): HttpRequest<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Number(blockNumber))

        return getBlockWithTxs(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionHashesWithBlockTag
     */
    override fun getBlockWithTxHashes(blockTag: BlockTag): HttpRequest<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Tag(blockTag))

        return getBlockWithTxHashes(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionHashesWithBlockHash
     */
    override fun getBlockWithTxHashes(blockHash: Felt): HttpRequest<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Hash(blockHash))

        return getBlockWithTxHashes(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionHashesWithBlockNumber
     */
    override fun getBlockWithTxHashes(blockNumber: Int): HttpRequest<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Number(blockNumber))

        return getBlockWithTxHashes(payload)
    }

    private fun getBlockWithTxHashes(payload: GetBlockWithTransactionHashesPayload): HttpRequest<BlockWithTransactionHashes> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TX_HASHES, jsonPayload, BlockWithTransactionHashesPolymorphicSerializer)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionReceiptsWithBlockTag
     */
    override fun getBlockWithReceipts(blockTag: BlockTag): HttpRequest<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Tag(blockTag))

        return getBlockWithReceipts(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionReceiptsWithBlockHash
     */
    override fun getBlockWithReceipts(blockHash: Felt): HttpRequest<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Hash(blockHash))

        return getBlockWithReceipts(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getBlockWithTransactionReceiptsWithBlockNumber
     */
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

    /**
     * @sample starknet.provider.ProviderTest.getStateOfBlockWithLatestTag
     */
    override fun getStateUpdate(blockTag: BlockTag): HttpRequest<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Tag(blockTag))

        return getStateUpdate(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getStateOfBlockWithHash
     */
    override fun getStateUpdate(blockHash: Felt): HttpRequest<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Hash(blockHash))

        return getStateUpdate(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getStateOfBlockWithNumber
     */
    override fun getStateUpdate(blockNumber: Int): HttpRequest<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Number(blockNumber))

        return getStateUpdate(payload)
    }

    private fun getTransactionByBlockIdAndIndex(payload: GetTransactionByBlockIdAndIndexPayload): HttpRequest<Transaction> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_BLOCK_ID_AND_INDEX, jsonPayload, TransactionSerializer)
    }

    /**
     * @sample starknet.provider.ProviderTest.getTransactionByBlockTagAndIndex
     */
    override fun getTransactionByBlockIdAndIndex(blockTag: BlockTag, index: Int): HttpRequest<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Tag(blockTag), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getTransactionByBlockHashAndIndex
     */
    override fun getTransactionByBlockIdAndIndex(blockHash: Felt, index: Int): HttpRequest<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Hash(blockHash), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    /**
     * @sample starknet.provider.ProviderTest.getTransactionByBlockNumberAndIndex
     */
    override fun getTransactionByBlockIdAndIndex(blockNumber: Int, index: Int): HttpRequest<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Number(blockNumber), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    private fun simulateTransactions(payload: SimulateTransactionsPayload): HttpRequest<SimulatedTransactionList> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.SIMULATE_TRANSACTIONS, params, SimulatedTransactionListSerializer)
    }

    /**
     * @sample starknet.account.StandardAccountTest.SimulateTransactionsTest.simulateInvokeV3AndDeployAccountV3Transactions
     */
    override fun simulateTransactions(
        transactions: List<ExecutableTransaction>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlag>,
    ): HttpRequest<SimulatedTransactionList> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Tag(blockTag), simulationFlags)

        return simulateTransactions(payload)
    }

    override fun simulateTransactions(
        transactions: List<ExecutableTransaction>,
        blockNumber: Int,
        simulationFlags: Set<SimulationFlag>,
    ): HttpRequest<SimulatedTransactionList> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Number(blockNumber), simulationFlags)

        return simulateTransactions(payload)
    }

    override fun simulateTransactions(
        transactions: List<ExecutableTransaction>,
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
    GET_MESSAGES_STATUS("starknet_getMessagesStatus"),
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
    GET_STORAGE_PROOF("starknet_getStorageProof"),
    DEPLOY_ACCOUNT_TRANSACTION("starknet_addDeployAccountTransaction"),
    SIMULATE_TRANSACTIONS("starknet_simulateTransactions"),
}
