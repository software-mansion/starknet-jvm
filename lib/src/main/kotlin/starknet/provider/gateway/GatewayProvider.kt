package starknet.provider

import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import starknet.data.types.*
import starknet.provider.http.HttpRequest

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

    override fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
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

        return HttpRequest(url, "POST", emptyList(), jsonPayload.toString(), CallContractResponse.serializer())
    }

    override fun getStorageAt(payload: GetStorageAtPayload): Request<GetStorageAtResponse> {
        val params = buildList {
            add(Pair("contractAddress", payload.contractAddress.hexString()))
            add(Pair("key", payload.key.hexString()))
            add(Pair("blockHash", payload.blockHashOrTag.string()))
        }

        val url = buildRequestUrl(feederGatewayUrl, "get_storage_at", params)

        return HttpRequest(url, "GET", emptyList(), "", GetStorageAtResponse.serializer())
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

        return HttpRequest(url, "POST", emptyList(), jsonPayload.toString(), InvokeFunctionResponse.serializer())
    }
}
