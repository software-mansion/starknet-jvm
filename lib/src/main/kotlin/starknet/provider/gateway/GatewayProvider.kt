package starknet.provider.gateway

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import starknet.data.types.*
import starknet.provider.Provider
import starknet.provider.Request
import starknet.service.http.HttpRequest
import starknet.service.http.HttpService

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
    override val chainId: StarknetChainId
) : Provider {
    private fun mapCalldataToDecimal(calldata: Calldata): JsonElement {
        return JsonArray(calldata.map {
            JsonPrimitive(it.decString())
        })
    }

    private fun buildRequestUrl(
        baseUrl: String,
        endpoint: String,
        params: List<Pair<String, String>>? = emptyList()
    ): String {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        urlBuilder.addPathSegment(endpoint)

        if (params != null) {
            for (param in params) {
                urlBuilder.addQueryParameter(param.first, param.second)
            }
        }

        return urlBuilder.build().toString()
    }

    private fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
        val params = buildList {
            add(Pair("blockHash", payload.blockHashOrTag.string()))
        }

        val url = buildRequestUrl(feederGatewayUrl, "call_contract", params)

        val decimalCalldata = mapCalldataToDecimal(payload.request.calldata)

        val jsonPayload = Json.encodeToJsonElement(
            mapOf(
                "contract_address" to payload.request.contractAddress,
                "entry_point_selector" to payload.request.entrypoint,
                "calldata" to decimalCalldata,
            )
        )

        val payload = HttpService.Payload(url, "POST", emptyList(), jsonPayload.toString())
        return HttpRequest(payload, CallContractResponse.serializer())
    }

    override fun callContract(call: Call, blockTag: BlockTag): Request<CallContractResponse> {
        val payload = CallContractPayload(call, BlockHashOrTag.Tag(blockTag))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockHash: Felt): Request<CallContractResponse> {
        val payload = CallContractPayload(call, BlockHashOrTag.Hash(blockHash))

        return callContract(payload)
    }

    private fun getStorageAt(payload: GetStorageAtPayload): Request<GetStorageAtResponse> {
        val params = buildList {
            add(Pair("contractAddress", payload.contractAddress.hexString()))
            add(Pair("key", payload.key.hexString()))
            add(Pair("blockHash", payload.blockHashOrTag.string()))
        }

        val url = buildRequestUrl(feederGatewayUrl, "get_storage_at", params)

        val payload = HttpService.Payload(url, "GET", emptyList(), "")
        return HttpRequest(payload, GetStorageAtResponse.serializer())
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<GetStorageAtResponse> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockHashOrTag.Tag(blockTag))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<GetStorageAtResponse> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockHashOrTag.Hash(blockHash))

        return getStorageAt(payload)
    }

    override fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse> {
        val url = buildRequestUrl(gatewayUrl, "add_transaction")

        val decimalCalldata = mapCalldataToDecimal(payload.invocation.calldata)

        val jsonPayload = Json.encodeToJsonElement(
            mapOf(
                "type" to JsonPrimitive("INVOKE_FUNCTION"),
                "contract_address" to payload.invocation.contractAddress,
                "entry_point_selector" to payload.invocation.entrypoint,
                "calldata" to decimalCalldata,
                "max_fee" to payload.maxFee,
                "signature" to payload.signature
            )
        )

        val payload = HttpService.Payload(url, "POST", emptyList(), jsonPayload.toString())
        return HttpRequest(payload, InvokeFunctionResponse.serializer())
    }
}
