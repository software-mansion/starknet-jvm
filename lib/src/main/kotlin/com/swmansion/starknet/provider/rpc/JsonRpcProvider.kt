package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.*
import com.swmansion.starknet.data.serializers.BlockWithTransactionsPolymorphicSerializer
import com.swmansion.starknet.data.serializers.SyncPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionPolymorphicSerializer
import com.swmansion.starknet.data.serializers.TransactionReceiptPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.service.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
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

    private fun buildRequestJson(method: String, paramsJson: JsonElement, id: Int = 0): Map<String, JsonElement> {
        val map = mapOf(
            "jsonrpc" to JsonPrimitive("2.0"),
            "method" to JsonPrimitive(method),
            "id" to JsonPrimitive(id),
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

        return HttpRequest(payload, buildJsonHttpDeserializer(responseSerializer, deserializationJson), httpService)
    }

    fun <T> sendBatchRpcRequest(
        calls: List<Request<T>>,
    ): BatchHttpRequest<T> {
        val httpRequests = calls.map { it as HttpRequest<T> }
        val updatedJsonObjects = httpRequests.mapIndexed { index, request ->
            val jsonMap = Json.parseToJsonElement(request.payload.body.toString()).jsonObject.toMutableMap()
            jsonMap["id"] = JsonPrimitive(index)
            JsonObject(jsonMap)
        }

        val jsonArray = JsonArray(updatedJsonObjects)
        val deserializer = httpRequests.first().deserializer
        val payload = HttpService.Payload(
            url,
            "POST",
            emptyList(),
            jsonArray.toString(),
        )

        return BatchHttpRequest(payload, deserializer, httpService)
    }

    override fun getSpecVersion(): Request<String> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(JsonRpcMethod.GET_SPEC_VERSION, params, String.serializer())
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

    override fun callContract(call: Call): Request<List<Felt>> {
        return callContract(call, BlockTag.LATEST)
    }

    override fun deployAccount(payload: DeployAccountTransactionV1Payload): Request<DeployAccountResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("deploy_account_transaction", params)
        }

        return buildRequest(JsonRpcMethod.DEPLOY_ACCOUNT_TRANSACTION, jsonPayload, DeployAccountResponse.serializer())
    }

    override fun deployAccount(payload: DeployAccountTransactionV3Payload): Request<DeployAccountResponse> {
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

    override fun getStorageAt(contractAddress: Felt, key: Felt): Request<Felt> {
        return getStorageAt(contractAddress, key, BlockTag.LATEST)
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
            TransactionReceiptPolymorphicSerializer,
        )
    }

    override fun getTransactionStatus(transactionHash: Felt): Request<GetTransactionStatusResponse> {
        val payload = GetTransactionStatusPayload(transactionHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_STATUS, params, GetTransactionStatusResponse.serializer())
    }

    override fun invokeFunction(
        payload: InvokeTransactionV1Payload,
    ): Request<InvokeFunctionResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("invoke_transaction", params)
        }

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, jsonPayload, InvokeFunctionResponse.serializer())
    }

    override fun invokeFunction(
        payload: InvokeTransactionV3Payload,
    ): Request<InvokeFunctionResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("invoke_transaction", params)
        }

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, jsonPayload, InvokeFunctionResponse.serializer())
    }

    private fun getClass(payload: GetClassPayload): Request<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS, params, ContractClassPolymorphicSerializer)
    }

    override fun getClass(classHash: Felt): Request<ContractClassBase> {
        return getClass(classHash, BlockTag.LATEST)
    }

    override fun getClass(classHash: Felt, blockHash: Felt): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Hash(blockHash))

        return getClass(payload)
    }

    override fun getClass(classHash: Felt, blockNumber: Int): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Number(blockNumber))

        return getClass(payload)
    }

    override fun getClass(classHash: Felt, blockTag: BlockTag): Request<ContractClassBase> {
        val payload = GetClassPayload(classHash, BlockId.Tag(blockTag))

        return getClass(payload)
    }

    private fun getClassAt(payload: GetClassAtPayload): Request<ContractClassBase> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_AT, params, ContractClassPolymorphicSerializer)
    }

    override fun getClassAt(contractAddress: Felt, blockHash: Felt): Request<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Hash(blockHash), contractAddress)

        return getClassAt(payload)
    }

    override fun getClassAt(contractAddress: Felt, blockNumber: Int): Request<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Number(blockNumber), contractAddress)

        return getClassAt(payload)
    }

    override fun getClassAt(contractAddress: Felt, blockTag: BlockTag): Request<ContractClassBase> {
        val payload = GetClassAtPayload(BlockId.Tag(blockTag), contractAddress)

        return getClassAt(payload)
    }

    override fun getClassAt(contractAddress: Felt): Request<ContractClassBase> {
        return getClassAt(contractAddress, BlockTag.LATEST)
    }

    private fun getClassHashAt(payload: GetClassAtPayload): Request<Felt> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_HASH_AT, params, Felt.serializer())
    }

    override fun getClassHashAt(contractAddress: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetClassAtPayload(BlockId.Hash(blockHash), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt, blockNumber: Int): Request<Felt> {
        val payload = GetClassAtPayload(BlockId.Number(blockNumber), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetClassAtPayload(BlockId.Tag(blockTag), contractAddress)

        return getClassHashAt(payload)
    }

    override fun getClassHashAt(contractAddress: Felt): Request<Felt> {
        return getClassHashAt(contractAddress, BlockTag.LATEST)
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

    override fun declareContract(payload: DeclareTransactionV3Payload): Request<DeclareResponse> {
        val params = jsonWithDefaults.encodeToJsonElement(DeclareTransactionV3PayloadSerializer, payload)
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

    override fun getEvents(payload: GetEventsPayload): Request<GetEventsResult> {
        val params = Json.encodeToJsonElement(payload)
        val jsonPayload = buildJsonObject {
            put("filter", params)
        }

        return buildRequest(JsonRpcMethod.GET_EVENTS, jsonPayload, GetEventsResult.serializer())
    }

    private fun getEstimateFee(payload: EstimateTransactionFeePayload): Request<List<EstimateFeeResponse>> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_FEE, jsonPayload, ListSerializer(EstimateFeeResponse.serializer()))
    }

    override fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockHash: Felt,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Hash(blockHash))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockHash: Felt,
    ): Request<List<EstimateFeeResponse>> {
        return getEstimateFee(payload, blockHash, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockNumber: Int,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Number(blockNumber))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockNumber: Int,
    ): Request<List<EstimateFeeResponse>> {
        return getEstimateFee(payload, blockNumber, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockTag: BlockTag,
        simulationFlags: Set<SimulationFlagForEstimateFee>,
    ): Request<List<EstimateFeeResponse>> {
        val estimatePayload = EstimateTransactionFeePayload(payload, simulationFlags, BlockId.Tag(blockTag))

        return getEstimateFee(estimatePayload)
    }

    override fun getEstimateFee(
        payload: List<TransactionPayload>,
        blockTag: BlockTag,
    ): Request<List<EstimateFeeResponse>> {
        return getEstimateFee(payload, blockTag, defaultFeeEstimateSimulationFlags)
    }

    override fun getEstimateFee(payload: List<TransactionPayload>, simulationFlags: Set<SimulationFlagForEstimateFee>): Request<List<EstimateFeeResponse>> {
        return getEstimateFee(payload, BlockTag.PENDING, simulationFlags)
    }

    override fun getEstimateFee(payload: List<TransactionPayload>): Request<List<EstimateFeeResponse>> {
        return getEstimateFee(payload, BlockTag.PENDING, defaultFeeEstimateSimulationFlags)
    }

    private fun getEstimateMessageFee(payload: EstimateMessageFeePayload): Request<EstimateFeeResponse> {
        val jsonPayload = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.ESTIMATE_MESSAGE_FEE, jsonPayload, EstimateFeeResponse.serializer())
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockHash: Felt): Request<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Hash(blockHash))

        return getEstimateMessageFee(estimatePayload)
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockNumber: Int): Request<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Number(blockNumber))

        return getEstimateMessageFee(estimatePayload)
    }

    override fun getEstimateMessageFee(message: MessageL1ToL2, blockTag: BlockTag): Request<EstimateFeeResponse> {
        val estimatePayload = EstimateMessageFeePayload(message, BlockId.Tag(blockTag))

        return getEstimateMessageFee(estimatePayload)
    }

    private fun getNonce(payload: GetNoncePayload): Request<Felt> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_NONCE, jsonPayload, Felt.serializer())
    }

    override fun getNonce(contractAddress: Felt): Request<Felt> =
        getNonce(contractAddress, blockTag = BlockTag.PENDING)

    override fun getNonce(contractAddress: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Tag(blockTag))

        return getNonce(payload)
    }

    override fun getNonce(contractAddress: Felt, blockNumber: Int): Request<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Number(blockNumber))

        return getNonce(payload)
    }

    override fun getNonce(contractAddress: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetNoncePayload(contractAddress, BlockId.Hash(blockHash))

        return getNonce(payload)
    }

    override fun getSyncing(): Request<Syncing> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(
            JsonRpcMethod.GET_SYNCING,
            params,
            SyncPolymorphicSerializer,
        )
    }

    override fun getChainId(): Request<StarknetChainId> {
        val params = Json.encodeToJsonElement(JsonArray(emptyList()))

        return buildRequest(JsonRpcMethod.GET_CHAIN_ID, params, StarknetChainId.serializer())
    }

    private fun getBlockWithTxs(payload: GetBlockWithTransactionsPayload): Request<BlockWithTransactions> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TXS, jsonPayload, BlockWithTransactionsPolymorphicSerializer)
    }

    override fun getBlockWithTxs(blockTag: BlockTag): Request<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Tag(blockTag))

        return getBlockWithTxs(payload)
    }

    override fun getBlockWithTxs(blockHash: Felt): Request<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Hash(blockHash))

        return getBlockWithTxs(payload)
    }

    override fun getBlockWithTxs(blockNumber: Int): Request<BlockWithTransactions> {
        val payload = GetBlockWithTransactionsPayload(BlockId.Number(blockNumber))

        return getBlockWithTxs(payload)
    }

    override fun getBlockWithTxHashes(blockTag: BlockTag): Request<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Tag(blockTag))

        return getBlockWithTxHashes(payload)
    }

    override fun getBlockWithTxHashes(blockHash: Felt): Request<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Hash(blockHash))

        return getBlockWithTxHashes(payload)
    }

    override fun getBlockWithTxHashes(blockNumber: Int): Request<BlockWithTransactionHashes> {
        val payload = GetBlockWithTransactionHashesPayload(BlockId.Number(blockNumber))

        return getBlockWithTxHashes(payload)
    }

    private fun getBlockWithTxHashes(payload: GetBlockWithTransactionHashesPayload): Request<BlockWithTransactionHashes> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_TX_HASHES, jsonPayload, BlockWithTransactionHashesPolymorphicSerializer)
    }

    override fun getBlockWithReceipts(blockTag: BlockTag): Request<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Tag(blockTag))

        return getBlockWithReceipts(payload)
    }

    override fun getBlockWithReceipts(blockHash: Felt): Request<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Hash(blockHash))

        return getBlockWithReceipts(payload)
    }

    override fun getBlockWithReceipts(blockNumber: Int): Request<BlockWithReceipts> {
        val payload = GetBlockWithReceiptsPayload(BlockId.Number(blockNumber))

        return getBlockWithReceipts(payload)
    }

    private fun getBlockWithReceipts(payload: GetBlockWithReceiptsPayload): Request<BlockWithReceipts> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_BLOCK_WITH_RECEIPTS, jsonPayload, BlockWithReceiptsPolymorphicSerializer)
    }

    private fun getStateUpdate(payload: GetStateUpdatePayload): Request<StateUpdate> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STATE_UPDATE, jsonPayload, StateUpdatePolymorphicSerializer)
    }

    override fun getStateUpdate(blockTag: BlockTag): Request<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Tag(blockTag))

        return getStateUpdate(payload)
    }

    override fun getStateUpdate(blockHash: Felt): Request<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Hash(blockHash))

        return getStateUpdate(payload)
    }

    override fun getStateUpdate(blockNumber: Int): Request<StateUpdate> {
        val payload = GetStateUpdatePayload(BlockId.Number(blockNumber))

        return getStateUpdate(payload)
    }

    private fun getTransactionByBlockIdAndIndex(payload: GetTransactionByBlockIdAndIndexPayload): Request<Transaction> {
        val jsonPayload = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_BLOCK_ID_AND_INDEX, jsonPayload, TransactionPolymorphicSerializer)
    }

    override fun getTransactionByBlockIdAndIndex(blockTag: BlockTag, index: Int): Request<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Tag(blockTag), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    override fun getTransactionByBlockIdAndIndex(blockHash: Felt, index: Int): Request<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Hash(blockHash), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    override fun getTransactionByBlockIdAndIndex(blockNumber: Int, index: Int): Request<Transaction> {
        val payload = GetTransactionByBlockIdAndIndexPayload(BlockId.Number(blockNumber), index)

        return getTransactionByBlockIdAndIndex(payload)
    }

    private fun simulateTransactions(payload: SimulateTransactionsPayload): Request<List<SimulatedTransaction>> {
        val params = jsonWithDefaults.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.SIMULATE_TRANSACTIONS, params, ListSerializer(SimulatedTransaction.serializer()))
    }

    override fun simulateTransactions(transactions: List<TransactionPayload>, blockTag: BlockTag, simulationFlags: Set<SimulationFlag>): Request<List<SimulatedTransaction>> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Tag(blockTag), simulationFlags)

        return simulateTransactions(payload)
    }

    override fun simulateTransactions(transactions: List<TransactionPayload>, blockNumber: Int, simulationFlags: Set<SimulationFlag>): Request<List<SimulatedTransaction>> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Number(blockNumber), simulationFlags)

        return simulateTransactions(payload)
    }

    override fun simulateTransactions(transactions: List<TransactionPayload>, blockHash: Felt, simulationFlags: Set<SimulationFlag>): Request<List<SimulatedTransaction>> {
        val payload = SimulateTransactionsPayload(transactions, BlockId.Hash(blockHash), simulationFlags)

        return simulateTransactions(payload)
    }
}

interface RcpResponse

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
