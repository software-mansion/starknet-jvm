package com.swmansion.starknet.provider.gateway

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
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import java.util.function.Function

/**
 * A provider for interacting with Starknet gateway. You should reuse it in your application to share the
 * httpService or provide it with your own httpService.
 *
 * @param feederGatewayUrl url of the feeder gateway
 * @param gatewayUrl url of the gateway
 * @param chainId an id of the network
 * @param httpService service used for making http requests
 */
@Deprecated("Consider using JsonRpcProvider instead.")
class GatewayProvider(
    val feederGatewayUrl: String,
    val gatewayUrl: String,
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

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    private data class GatewayError(
        @JsonNames("message")
        val message: String,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun handleResponseError(response: HttpResponse): String {
        if (!response.isSuccessful) {
            try {
                val deserializedError =
                    json.decodeFromString(GatewayError.serializer(), response.body)
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

    private fun <T> buildDeserializer(
        deserializationStrategy: DeserializationStrategy<T>,
    ): HttpResponseDeserializer<T> =
        Function { response ->
            val body = handleResponseError(response)

            json.decodeFromString(deserializationStrategy, body)
        }

    private fun handleMissingTransaction(response: HttpResponse): String {
        val body = handleResponseError(response)

        @Serializable
        data class MissingTransactionResponse(val status: TransactionStatus)

        val missingTransaction = json.decodeFromString(MissingTransactionResponse.serializer(), body)
        if (missingTransaction.status == TransactionStatus.UNKNOWN) throw GatewayRequestFailedException(
            message = "Transaction not received or unknown",
            payload = body,
        )

        return body
    }

    private fun getStorageAt(payload: GetStorageAtPayload): Request<Felt> {
        val params = listOf(
            Pair("contractAddress", payload.contractAddress.hexString()),
            Pair("key", payload.key.decString()),
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
            { response ->
                val body = handleMissingTransaction(response)
                json.decodeFromString(GatewayTransactionTransformingSerializer, body)
            },
            httpService,
        )
    }

    override fun getTransactionReceipt(transactionHash: Felt): Request<out TransactionReceipt> {
        val url = feederGatewayRequestUrl("get_transaction_receipt")
        val params = listOf(Pair("transactionHash", transactionHash.hexString()))

        return HttpRequest(
            Payload(url, "GET", params),
            { response ->
                val body = handleMissingTransaction(response)
                json.decodeFromString(GatewayTransactionReceipt.serializer(), body)
            },
            httpService,
        )
    }

    override fun invokeFunction(payload: InvokeTransactionPayload): Request<InvokeFunctionResponse> {
        val url = gatewayRequestUrl("add_transaction")
        val body = serializeInvokeTransactionPayload(payload)

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(InvokeFunctionResponse.serializer()),
            httpService,
        )
    }

    override fun declareContract(payload: DeclareTransactionV1Payload): Request<DeclareResponse> {
        val url = gatewayRequestUrl("add_transaction")

        val body = buildJsonObject {
            put("type", payload.type.toString())
            put("sender_address", payload.senderAddress)
            put("max_fee", payload.maxFee)
            put("nonce", payload.nonce)
            put("version", payload.version)
            putJsonArray("signature") { payload.signature.toDecimal().forEach { add(it) } }
            put("contract_class", payload.contractDefinition.toJson())
        }

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(DeclareResponse.serializer()),
            httpService,
        )
    }

    override fun declareContract(payload: DeclareTransactionV2Payload): Request<DeclareResponse> {
        val url = gatewayRequestUrl("add_transaction")

        val body = buildJsonObject {
            put("type", payload.type.toString())
            put("sender_address", payload.senderAddress)
            put("max_fee", payload.maxFee)
            put("nonce", payload.nonce)
            put("version", payload.version)
            putJsonArray("signature") { payload.signature.toDecimal().forEach { add(it) } }
            put("contract_class", payload.contractDefinition.toGatewayJson())
            put("compiled_class_hash", payload.compiledClassHash)
        }

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(DeclareResponse.serializer()),
            httpService,
        )
    }

    override fun deployAccount(payload: DeployAccountTransactionPayload): Request<DeployAccountResponse> {
        val url = gatewayRequestUrl("add_transaction")
        val body = serializeDeployAccountTransactionPayload(payload)

        return HttpRequest(
            Payload(url, "POST", body),
            buildDeserializer(DeployAccountResponse.serializer()),
            httpService,
        )
    }

    override fun getClass(classHash: Felt): Request<ContractClassBase> {
        val url = feederGatewayRequestUrl("get_class_by_hash")

        val params = listOf(
            "classHash" to classHash.hexString(),
        )

        val httpPayload = Payload(url, "GET", params)
        return HttpRequest(httpPayload, buildDeserializer(GatewayContractClassPolymorphicSerializer), httpService)
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

    private fun serializeDeployAccountTransactionPayload(
        payload: DeployAccountTransactionPayload,
    ): JsonObject =
        buildJsonObject {
            put("type", "DEPLOY_ACCOUNT")
            put("class_hash", payload.classHash)
            put("contract_address_salt", payload.salt)
            putJsonArray("constructor_calldata") {
                payload.constructorCalldata.toDecimal().forEach { add(it) }
            }
            put("version", payload.version)
            put("nonce", payload.nonce)
            put("max_fee", payload.maxFee)
            putJsonArray("signature") {
                payload.signature.toDecimal().forEach { add(it) }
            }
        }

    private fun serializeInvokeTransactionPayload(
        payload: InvokeTransactionPayload,
    ): JsonObject =
        buildJsonObject {
            put("type", "INVOKE_FUNCTION")
            put("contract_address", payload.senderAddress.hexString())
            putJsonArray("calldata") { payload.calldata.toDecimal().forEach { add(it) } }
            putJsonArray("signature") { payload.signature.toDecimal().forEach { add(it) } }
            put("nonce", payload.nonce)
            put("version", payload.version)
            put("max_fee", payload.maxFee)
        }

    companion object Factory {
        @JvmStatic
        fun makeTestnetProvider(): GatewayProvider {
            return GatewayProvider(
                "$TESTNET_URL/feeder_gateway",
                "$TESTNET_URL/gateway",
                StarknetChainId.TESTNET,
            )
        }

        @JvmStatic
        fun makeTestnetProvider(httpService: HttpService): GatewayProvider {
            return GatewayProvider(
                "$TESTNET_URL/feeder_gateway",
                "$TESTNET_URL/gateway",
                StarknetChainId.TESTNET,
                httpService,
            )
        }

        @JvmStatic
        fun makeMainnetProvider(): GatewayProvider {
            return GatewayProvider(
                "$MAINNET_URL/feeder_gateway",
                "$MAINNET_URL/gateway",
                StarknetChainId.MAINNET,
            )
        }

        @JvmStatic
        fun makeMainnetProvider(httpService: HttpService): GatewayProvider {
            return GatewayProvider(
                "$MAINNET_URL/feeder_gateway",
                "$MAINNET_URL/gateway",
                StarknetChainId.MAINNET,
                httpService,
            )
        }
    }
}
