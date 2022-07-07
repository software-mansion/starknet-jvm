package starknet.provider.rpc

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import starknet.data.types.*
import starknet.provider.Provider
import starknet.provider.Request
import starknet.service.http.HttpRequest
import starknet.service.http.HttpService

class JsonRpcProvider(
    private val url: String,
    override val chainId: StarknetChainId
) : Provider {

    private fun buildRequestJson(method: String, paramsJson: JsonElement): Map<String, JsonElement> {
        val map = mapOf(
            "jsonrpc" to JsonPrimitive("2.0"),
            "method" to JsonPrimitive(method),
            "id" to JsonPrimitive(0), // It is not used anywhere
            "params" to paramsJson
        )

        return JsonObject(map)
    }

    private fun <T : Response> buildRequest(
        method: JsonRpcMethod,
        paramsJson: JsonElement,
        deserializer: DeserializationStrategy<T>
    ): HttpRequest<T> {
        val requestJson = buildRequestJson(method.methodName, paramsJson)

        val payload = HttpService.Payload(url, "POST", emptyList(), requestJson.toString())

        return HttpRequest(payload, deserializer)
    }

    override fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.CALL, params, CallContractResponse.serializer())
    }

    override fun getStorageAt(payload: GetStorageAtPayload): Request<GetStorageAtResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.GET_STORAGE_AT, params, GetStorageAtResponse.serializer())
    }

    override fun invokeFunction(
        payload: InvokeFunctionPayload
    ): Request<InvokeFunctionResponse> {
        val params = Json.encodeToJsonElement(payload)

        return buildRequest(JsonRpcMethod.INVOKE_TRANSACTION, params, InvokeFunctionResponse.serializer())
    }
}
