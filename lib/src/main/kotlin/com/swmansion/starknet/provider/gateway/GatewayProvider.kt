package com.swmansion.starknet.provider.gateway

import com.swmansion.starknet.data.DECLARE_SENDER_ADDRESS
import com.swmansion.starknet.data.NetUrls.MAINNET_URL
import com.swmansion.starknet.data.NetUrls.TESTNET_URL
import com.swmansion.starknet.data.serializers.GatewayCallContractTransformingSerializer
import com.swmansion.starknet.data.serializers.GatewayTransactionTransformingSerializer
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.extensions.put
import com.swmansion.starknet.extensions.toDecimal
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.service.http.*
import com.swmansion.starknet.service.http.HttpRequest
import com.swmansion.starknet.service.http.HttpResponseDeserializer
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.service.http.HttpService.Payload
import com.swmansion.starknet.service.http.OkhttpHttpService
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

/**
 * A provider for interacting with StarkNet gateway.
 *
 * @param feederGatewayUrl url of the feeder gateway
 * @param gatewayUrl url of the gateway
 * @param chainId an id of the network
 */
class GatewayProvider(
    private val feederGatewayUrl: String,
    private val gatewayUrl: String,
    override val chainId: StarknetChainId,
    private val httpService: HttpService = OkhttpHttpService(),
) : Provider {

    @Suppress("SameParameterValue")
    private fun gatewayRequestUrl(method: String): String {
        return "$gatewayUrl/$method"
    }

    private fun feederGatewayRequestUrl(method: String): String {
        return "$feederGatewayUrl/$method"
    }

    private fun callContract(payload: CallContractPayload): Request<List<Felt>> {
        val url = feederGatewayRequestUrl("call_contract")

        val params = listOf(Pair("blockHash", payload.blockId.toString()))

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

    @Serializable
    private data class GatewayError(
        @SerialName("status_code")
        val code: Int,

        @SerialName("message")
        val message: String,
    )

    private fun handleResponseError(response: HttpResponse): String {
        if (!response.isSuccessful) {
            try {
                val deserializedError = Json.decodeFromString(GatewayError.serializer(), response.body)
                throw GatewayRequestFailedException(
                    code = deserializedError.code,
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
            Pair("blockHash", payload.blockId.toString()),
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
            put("type", JsonPrimitive("INVOKE_FUNCTION"))
            put("contract_address", payload.invocation.contractAddress.hexString())
            put("entry_point_selector", payload.invocation.entrypoint.hexString())
            putJsonArray("calldata") {
                payload.invocation.calldata.toDecimal().forEach { add(it) }
            }
            put("max_fee", payload.maxFee.hexString())
            putJsonArray("signature") {
                payload.signature.toDecimal().forEach { add(it) }
            }
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
            put("type", "DEPLOY")
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
            put("type", "DECLARE")
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

    private fun getEstimateFee(request: InvokeTransaction, blockParam: Pair<String, String>): Request<EstimateFeeResponse> {
        val url = feederGatewayRequestUrl("estimate_fee")

        val body = buildJsonObject {
            put("contract_address", request.contractAddress)
            put("entry_point_selector", request.entryPointSelector)
            put("max_fee", request.maxFee)
            putJsonArray("calldata") { request.calldata.toDecimal().forEach { add(it) } }
            putJsonArray("signature") { request.signature.toDecimal().forEach { add(it) } }
        }

        val httpPayload = Payload(url, "POST", listOf(blockParam), body)

        return HttpRequest(
            httpPayload,
            buildDeserializer(EstimateFeeResponseGatewaySerializer()),
            httpService,
        )
    }

    override fun getEstimateFee(request: InvokeTransaction, blockHash: Felt): Request<EstimateFeeResponse> {
        val param = "blockHash" to blockHash.hexString()

        return getEstimateFee(request, param)
    }

    override fun getEstimateFee(request: InvokeTransaction, blockNumber: Int): Request<EstimateFeeResponse> {
        val param = "blockNumber" to blockNumber.toString()

        return getEstimateFee(request, param)
    }

    override fun getEstimateFee(request: InvokeTransaction, blockTag: BlockTag): Request<EstimateFeeResponse> {
        val param = "blockTag" to blockTag.tag

        return getEstimateFee(request, param)
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
        fun makeMainnetClient(): GatewayProvider {
            return GatewayProvider(
                "$MAINNET_URL/feeder_gateway",
                "$MAINNET_URL/gateway",
                StarknetChainId.MAINNET,
            )
        }
    }
}
