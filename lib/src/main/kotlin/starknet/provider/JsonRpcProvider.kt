package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import starknet.data.types.*
import java.util.concurrent.atomic.AtomicLong

class JsonRpcProvider(
    val url: String,
    override val chainId: StarknetChainId
) : Provider {
    companion object {
        private val nextId = AtomicLong(0)
    }

    fun buildRequestJson(id: Long, method: String, params: List<JsonElement>): Map<String, JsonElement> {
        val map = mapOf(
            "jsonrpc" to JsonPrimitive("2.0"),
            "method" to JsonPrimitive(method),
            "id" to JsonPrimitive(id),
            "params" to JsonArray(params)
        )

        return JsonObject(map)
    }

    private fun <T : Response> buildRequest(
        method: JsonRpcMethod,
        params: List<JsonElement>,
        deserializer: DeserializationStrategy<T>
    ): Request<T> {
        val id = nextId.getAndIncrement()

        val requestJson = buildRequestJson(id, method.value, params)

        return Request(url, "POST", emptyList(), requestJson.toString(), deserializer)
    }

    override fun callContract(call: Call, callParams: CallExtraParams): Request<CallContractResponse> {
        val request = Json.encodeToJsonElement(call)

        val params = buildList {
            add(request)
            add(JsonPrimitive(callParams.blockHashOrTag.string()))
        }

        return buildRequest(JsonRpcMethod.CALL, params, CallContractResponse.serializer()) // TODO: Wrong deserializer
    }
}