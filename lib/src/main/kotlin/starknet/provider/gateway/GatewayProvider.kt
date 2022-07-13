package starknet.provider.gateway

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import starknet.data.types.*
import starknet.provider.Provider
import starknet.provider.Request
import starknet.service.http.HttpRequest
import starknet.service.http.HttpService

class GatewayProvider(
    private val feederGatewayUrl: String,
    private val gatewayUrl: String,
    override val chainId: StarknetChainId
) : Provider {
    private fun mapFeltListToDecimal(calldata: List<Felt>): JsonElement {
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
        val params = listOf(
            Pair("blockHash", payload.blockHashOrTag.string())
        )

        val url = buildRequestUrl(feederGatewayUrl, "call_contract", params)

        val decimalCalldata = mapFeltListToDecimal(payload.request.calldata)

        val jsonPayload = buildJsonObject {
            put("contract_address", payload.request.contractAddress.hexString())
            put("entry_point_selector", payload.request.entrypoint.hexString())
            put("calldata", decimalCalldata)
            put("signature", JsonArray(emptyList()))
        }

        val httpPayload = HttpService.Payload(url, "POST", emptyList(), jsonPayload.toString())
        return HttpRequest(httpPayload, CallContractResponse.serializer())
    }

    override fun callContract(call: Call, blockTag: BlockTag): Request<CallContractResponse> {
        val payload = CallContractPayload(call, BlockHashOrTag.Tag(blockTag))

        return callContract(payload)
    }

    override fun callContract(call: Call, blockHash: Felt): Request<CallContractResponse> {
        val payload = CallContractPayload(call, BlockHashOrTag.Hash(blockHash))

        return callContract(payload)
    }

    private fun getStorageAt(payload: GetStorageAtPayload): Request<Felt> {
        val params = listOf(
            Pair("contractAddress", payload.contractAddress.hexString()),
            Pair("key", payload.key.hexString()),
            Pair("blockHash", payload.blockHashOrTag.string())
        )

        val url = buildRequestUrl(feederGatewayUrl, "get_storage_at", params)

        val httpPayload = HttpService.Payload(url, "GET", emptyList(), null)
        return HttpRequest(httpPayload, Felt.serializer())
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockTag: BlockTag): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockHashOrTag.Tag(blockTag))

        return getStorageAt(payload)
    }

    override fun getStorageAt(contractAddress: Felt, key: Felt, blockHash: Felt): Request<Felt> {
        val payload = GetStorageAtPayload(contractAddress, key, BlockHashOrTag.Hash(blockHash))

        return getStorageAt(payload)
    }

    override fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse> {
        val url = buildRequestUrl(gatewayUrl, "add_transaction")

        val decimalCalldata = mapFeltListToDecimal(payload.invocation.calldata)

        val decimalSignature = mapFeltListToDecimal(payload.signature ?: emptyList())

        val jsonPayload = buildJsonObject {
            put("type", JsonPrimitive("INVOKE_FUNCTION"))
            put("contract_address", payload.invocation.contractAddress.hexString())
            put("entry_point_selector", payload.invocation.entrypoint.hexString())
            put("calldata", decimalCalldata)
            put("max_fee", payload.maxFee?.hexString())
            put("signature", decimalSignature)
        }

        val httpPayload = HttpService.Payload(url, "POST", emptyList(), jsonPayload.toString())
        return HttpRequest(httpPayload, InvokeFunctionResponse.serializer())
    }
}
