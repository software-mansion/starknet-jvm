package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.JsonRpcTransactionPolymorphicSerializer
import com.swmansion.starknet.data.serializers.JsonRpcTransactionReceiptPolymorphicSerializer
import com.swmansion.starknet.data.serializers.JsonRpcSyncPolymorphicSerializer
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.extensions.add
import com.swmansion.starknet.extensions.put
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpRequest
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.OkhttpHttpService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.*

/**
 * A provider for interacting with StarkNet JSON-RPC
 *
 * @param url url of the service providing a rpc interface
 * @param chainId an id of the network
 */
class JsonRpcProvider(
    private val url: String,
    override val chainId: StarknetChainId,
    private val httpService: HttpService = OkhttpHttpService(),
) : Provider {

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

        return buildRequest(JsonRpcMethod.GET_TRANSACTION_BY_HASH, params, JsonRpcTransactionPolymorphicSerializer)
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
        payload: InvokeFunctionPayload,
    ): Request<InvokeFunctionResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, params, InvokeFunctionResponse.serializer())
    }

    override fun getClass(classHash: Felt): Request<ContractClass> {
        val payload = GetClassPayload(classHash)
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS, params, ContractClass.serializer())
    }

    private fun getClassAt(payload: GetClassAtPayload): Request<ContractClass> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_CLASS_AT, params, ContractClass.serializer())
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition in the given block at the given address .
     *
     * @param blockHash The hash of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassAt(blockHash: Felt, contractAddress: Felt): Request<ContractClass> {
        val payload = GetClassAtPayload(blockHash.hexString(), contractAddress)

        return getClassAt(payload)
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition in the given block at the given address .
     *
     * @param blockNumber The number of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassAt(blockNumber: Int, contractAddress: Felt): Request<ContractClass> {
        val payload = GetClassAtPayload(blockNumber.toString(), contractAddress)

        return getClassAt(payload)
    }

    /**
     * Get the contract class definition.
     *
     * Get the contract class definition in the given block at the given address .
     *
     * @param blockTag The tag of the requested block.
     * @param contractAddress The address of the contract whose class definition will be returned.
     */
    fun getClassAt(blockTag: BlockTag, contractAddress: Felt): Request<ContractClass> {
        val payload = GetClassAtPayload(blockTag.tag, contractAddress)

        return getClassAt(payload)
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

    override fun deployContract(payload: DeployTransactionPayload): Request<DeployResponse> {
        val params = buildJsonObject {
            put("contract_definition", payload.contractDefinition.toRpcJson())
            putJsonArray("constructor_calldata") {
                payload.constructorCalldata.forEach { add(it) }
            }
            put("contract_address_salt", payload.salt)
        }

        return buildRequest(JsonRpcMethod.DEPLOY, params, DeployResponse.serializer())
    }

    override fun declareContract(payload: DeclareTransactionPayload): Request<DeclareResponse> {
        val params = buildJsonObject {
            put("contract_class", payload.contractDefinition.toRpcJson())
            put("version", payload.version)
        }

        return buildRequest(JsonRpcMethod.DECLARE, params, DeclareResponse.serializer())
    }

    override fun getBlockNumber(): Request<Int> {
        val params = Json.encodeToJsonElement(JsonArray(listOf()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_NUMBER,
            params,
            Int.serializer()
        )
    }

    override fun getBlockHashAndNumber(): Request<GetBlockHashAndNumberResponse> {
        val params = Json.encodeToJsonElement(JsonArray(listOf()))

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_HASH_AND_NUMBER,
            params,
            GetBlockHashAndNumberResponse.serializer()
        )
    }

    private fun getBlockTransactionCount(payload: GetBlockTransactionCountPayload): Request<Int> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(
            JsonRpcMethod.GET_BLOCK_TRANSACTION_COUNT,
            params,
            Int.serializer()
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

        return buildRequest(JsonRpcMethod.GET_EVENTS, params, GetEventsResult.serializer())

    /**
     * Get the block synchronization status.
     *
     * Get the starting, current and highest block info or boolean indicating syncing is not in progress.
     * 
     * @throws RequestFailedException
     */
    fun getSyncing(): Request<Syncing> {
        val params = Json.encodeToJsonElement(JsonArray(listOf()))

        return buildRequest(
            JsonRpcMethod.GET_SYNCING,
            params,
            JsonRpcSyncPolymorphicSerializer
        )
    }
}
