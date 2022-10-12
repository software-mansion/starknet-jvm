package com.swmansion.starknet.provider.gateway

import com.swmansion.starknet.data.DECLARE_SENDER_ADDRESS
import com.swmansion.starknet.data.NetUrls.MAINNET_URL
import com.swmansion.starknet.data.NetUrls.TESTNET_URL
import com.swmansion.starknet.data.serializers.*
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.extensions.put
import com.swmansion.starknet.extensions.toDecimal
import com.swmansion.starknet.extensions.toGatewayParam
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.service.http.*
import com.swmansion.starknet.service.http.HttpService.Payload
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * A provider for interacting with StarkNet gateway. You should reuse it in your application to share the
 * httpService or provide it with your own httpService.
 *
 * @param feederGatewayUrl url of the feeder gateway
 * @param gatewayUrl url of the gateway
 * @param chainId an id of the network
 * @param httpService service used for making http requests
 */
class GatewayProvider(
    private val feederGatewayUrl: String,
    private val gatewayUrl: String,
    override val chainId: StarknetChainId,
    private val httpService: HttpService,
) : Provider {
    constructor(feederGatewayUrl: String, gatewayUrl: String, chainId: StarknetChainId) : this(
        feederGatewayUrl,
        gatewayUrl,
        chainId,
        OkHttpService(),
    )

    @Suppress("SameParameterValue")
    private fun gatewayRequestUrl(method: String): String {
        return "$gatewayUrl/$method"
    }

    private fun feederGatewayRequestUrl(method: String): String {
        return "$feederGatewayUrl/$method"
    }

    private fun callContract(payload: CallContractPayload): Request<List<Felt>> {
        val url = feederGatewayRequestUrl("call_contract")

        val params = listOf(payload.blockId.toGatewayParam())

        val decimalCalldata = Json.encodeToJsonElement(payload.request.calldata.toDecimal())
        val body = buildJsonObject {
            put("contract_address", payload.request.contractAddress.hexString())
            put("entry_point_selector", payload.request.entrypoint.hexString())
            put("calldata", decimalCalldata)
            put("signature", JsonArray(emptyList()))
        }

        return HttpRequest(
            Payload(url, "POST", params, body),
            buildDeserializer(GatewayCallContractTransformingSerializer),
            httpService,
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private data class GatewayError(
        @JsonNames("message")
        val message: String,
    )

    private val jsonGatewayError = Json { ignoreUnknownKeys = true }

    private fun handleResponseError(response: HttpResponse): String {
        if (!response.isSuccessful) {
            try {
                val deserializedError =
                    jsonGatewayError.decodeFromString(GatewayError.serializer(), response.body)
                throw GatewayRequestFailedException(
                    message = deserializedError.message,
                    payload = response.body,
                )
            } catch (e: SerializationException) {
                throw RequestFailedException(payload = response.body)
            }
        }

        return response.body
    }

    private fun <T> buildDeserializer(deserializationStrategy: DeserializationStrategy<T>): HttpResponseDeserializer<T> =
        { response ->
            val body = handleResponseError(response)

            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString(deserializationStrategy, body)
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
        val params = listOf(
            Pair("contractAddress", payload.contractAddress.hexString()),
            Pair("key", payload.key.hexString()),
            payload.blockId.toGatewayParam(),
        )
        val url = feederGatewayRequestUrl("get_storage_at")

        return HttpRequest(Payload(url, "GET", params), buildDeserializer(Felt.serializer()), httpService)
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
        val url = feederGatewayRequestUrl("get_transaction")
        val params = listOf(Pair("transactionHash", transactionHash.hexString()))

        return HttpRequest(
            Payload(url, "GET", params),
            buildDeserializer(GatewayTransactionTransformingSerializer),
            httpService,
        )
    }

    override fun getTransactionReceipt(transactionHash: Felt): Request<out TransactionReceipt> {
        val url = feederGatewayRequestUrl("get_transaction_receipt")
        val params = listOf(Pair("transactionHash", transactionHash.hexString()))

        return HttpRequest(
            Payload(url, "GET", params),
            buildDeserializer(GatewayTransactionReceipt.serializer()),
            httpService,
        )
    }

    override fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse> {
        val url = gatewayRequestUrl("add_transaction")

        val body = buildJsonObject {
            put("type", transactionTypeToName(TransactionType.INVOKE))
            put("contract_address", payload.invocation.contractAddress.hexString())
            putJsonArray("calldata") { payload.invocation.calldata.toDecimal().forEach { add(it) } }
            put("max_fee", payload.maxFee.hexString())
            putJsonArray("signature") { payload.signature.toDecimal().forEach { add(it) } }
            put("nonce", payload.nonce)
            put("version", payload.version)
        }

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(InvokeFunctionResponse.serializer()),
            httpService,
        )
    }

    override fun deployContract(payload: DeployTransactionPayload): Request<DeployResponse> {
        val url = gatewayRequestUrl("add_transaction")

        val body = buildJsonObject {
            put("type", transactionTypeToName(TransactionType.DEPLOY))
            put("contract_address_salt", payload.salt)
            putJsonArray("constructor_calldata") {
                payload.constructorCalldata.toDecimal().forEach { add(it) }
            }
            put("contract_definition", payload.contractDefinition.toJson())
            put("version", payload.version)
        }

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(DeployResponse.serializer()),
            httpService,
        )
    }

    override fun declareContract(payload: DeclareTransactionPayload): Request<DeclareResponse> {
        val url = gatewayRequestUrl("add_transaction")

        val body = buildJsonObject {
            put("type", transactionTypeToName(TransactionType.DECLARE))
            put("sender_address", DECLARE_SENDER_ADDRESS)
            put("max_fee", payload.maxFee)
            put("nonce", payload.nonce)
            put("version", payload.version)
            putJsonArray("signature") { payload.signature }
            put("contract_class", payload.contractDefinition.toJson())
        }

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(DeclareResponse.serializer()),
            httpService,
        )
    }

    override fun getClass(classHash: Felt): Request<ContractClass> {
        val url = feederGatewayRequestUrl("get_class_by_hash")

        val params = listOf(
            "classHash" to classHash.hexString(),
        )

        val httpPayload = Payload(url, "GET", params)
        return HttpRequest(httpPayload, buildDeserializer(ContractClassGatewaySerializer), httpService)
    }

    private fun getClassHashAt(blockParam: Pair<String, String>, contractAddress: Felt): Request<Felt> {
        val url = feederGatewayRequestUrl("get_class_hash_at")
        val params = listOf(
            blockParam,
            "contractAddress" to contractAddress.hexString(),
        )

        val httpPayload = Payload(url, "GET", params)
        return HttpRequest(httpPayload, buildDeserializer(Felt.serializer()), httpService)
    }

    override fun getClassHashAt(contractAddress: Felt, blockHash: Felt): Request<Felt> {
        val param = "blockHash" to blockHash.hexString()

        return getClassHashAt(param, contractAddress)
    }

    override fun getClassHashAt(contractAddress: Felt, blockNumber: Int): Request<Felt> {
        val param = "blockNumber" to blockNumber.toString()

        return getClassHashAt(param, contractAddress)
    }

    override fun getClassHashAt(contractAddress: Felt, blockTag: BlockTag): Request<Felt> {
        val param = "blockTag" to blockTag.tag

        return getClassHashAt(param, contractAddress)
    }

    private fun getEstimateFee(
        request: InvokeTransaction,
        blockId: BlockId,
    ): Request<EstimateFeeResponse> {
        val url = feederGatewayRequestUrl("estimate_fee")
        val body = buildJsonObject {
            put("type", transactionTypeToName(request.type))
            put("contract_address", request.contractAddress.hexString())
            putJsonArray("calldata") { request.calldata.toDecimal().forEach { add(it) } }
            putJsonArray("signature") { request.signature.toDecimal().forEach { add(it) } }
            put("nonce", request.nonce)
            put("version", request.version)
        }

        val httpPayload = Payload(url, "POST", listOf(blockId.toGatewayParam()), body)

        return HttpRequest(
            httpPayload,
            buildDeserializer(EstimateFeeResponseGatewaySerializer),
            httpService,
        )
    }

    override fun getEstimateFee(request: InvokeTransaction, blockHash: Felt): Request<EstimateFeeResponse> {
        return getEstimateFee(request, BlockId.Hash(blockHash))
    }

    override fun getEstimateFee(request: InvokeTransaction, blockNumber: Int): Request<EstimateFeeResponse> {
        return getEstimateFee(request, BlockId.Number(blockNumber))
    }

    override fun getEstimateFee(request: InvokeTransaction, blockTag: BlockTag): Request<EstimateFeeResponse> {
        return getEstimateFee(request, BlockId.Tag(blockTag))
    }

    override fun getNonce(contractAddress: Felt): Request<Felt> = getNonce(contractAddress, BlockTag.PENDING)

    override fun getNonce(contractAddress: Felt, blockTag: BlockTag): Request<Felt> {
        val params = listOf(
            "contractAddress" to contractAddress.hexString(),
            BlockId.Tag(blockTag).toGatewayParam(),
        )
        val url = feederGatewayRequestUrl("get_nonce")

        return HttpRequest(Payload(url, "GET", params), buildDeserializer(Felt.serializer()), httpService)
    }

    override fun getBlockHashAndNumber(): Request<GetBlockHashAndNumberResponse> {
        val url = feederGatewayRequestUrl("get_block")
        val httpPayload = Payload(url, "GET")

        return HttpRequest(
            httpPayload,
            buildDeserializer(GetBlockHashAndNumberResponse.serializer()),
            httpService,
        )
    }

    override fun getBlockNumber(): Request<Int> {
        val url = feederGatewayRequestUrl("get_block")
        val httpPayload = Payload(url, "GET")

        return HttpRequest(
            httpPayload,
            buildDeserializer(GatewayGetBlockNumberSerializer),
            httpService,
        )
    }

    private fun getBlockTransactionCount(payload: GetBlockTransactionCountPayload): Request<Int> {
        val url = feederGatewayRequestUrl("get_block")
        val params = listOf(
            payload.blockId.toGatewayParam(),
        )

        val httpPayload = Payload(url, "GET", params)
        return HttpRequest(
            httpPayload,
            buildDeserializer(GatewayGetBlockTransactionCountSerializer),
            httpService,
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

    // Copied values from TransactionType in cairo-lang
    private fun transactionTypeToName(type: TransactionType) = when (type) {
        TransactionType.DECLARE -> "DECLARE"
        TransactionType.DEPLOY -> "DEPLOY"
        TransactionType.INVOKE -> "INVOKE_FUNCTION"
    }

    companion object Factory {
        @JvmStatic
        fun makeTestnetClient(): GatewayProvider {
            return GatewayProvider(
                "$TESTNET_URL/feeder_gateway",
                "$TESTNET_URL/gateway",
                StarknetChainId.TESTNET,
            )
        }

        @JvmStatic
        fun makeTestnetClient(httpService: HttpService): GatewayProvider {
            return GatewayProvider(
                "$TESTNET_URL/feeder_gateway",
                "$TESTNET_URL/gateway",
                StarknetChainId.TESTNET,
                httpService,
            )
        }

        @JvmStatic
        fun makeMainnetClient(): GatewayProvider {
            return GatewayProvider(
                "$MAINNET_URL/feeder_gateway",
                "$MAINNET_URL/gateway",
                StarknetChainId.MAINNET,
            )
        }

        @JvmStatic
        fun makeMainnetClient(httpService: HttpService): GatewayProvider {
            return GatewayProvider(
                "$MAINNET_URL/feeder_gateway",
                "$MAINNET_URL/gateway",
                StarknetChainId.MAINNET,
                httpService,
            )
        }
    }
}
