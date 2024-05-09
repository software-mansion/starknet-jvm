package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.JsonRpcErrorSerializer
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpResponseDeserializer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.function.Function

@Serializable
private data class JsonRpcResponse<T>(
    @SerialName("id")
    val id: Int,

    @SerialName("jsonrpc")
    val jsonRpc: String,

    @SerialName("result")
    val result: T? = null,

    @SerialName("error")
    val error: JsonRpcError? = null,
)

@Serializable(with = JsonRpcErrorSerializer::class)
internal data class JsonRpcError(
    @SerialName("code")
    val code: Int,

    @SerialName("message")
    val message: String,

    @SerialName("data")
    val data: String? = null,
)

private fun <T> extractResult(jsonRpcResponse: JsonRpcResponse<T>, fullPayload: String, payload: String = fullPayload): T {
    if (jsonRpcResponse.error != null) {
        throw RpcRequestFailedException(
            code = jsonRpcResponse.error.code,
            message = jsonRpcResponse.error.message,
            data = jsonRpcResponse.error.data,
            payload = payload,
        )
    }

    if (jsonRpcResponse.result == null) {
        throw RequestFailedException(message = "Response did not contain a result", payload = fullPayload)
    }
    return jsonRpcResponse.result
}

@JvmSynthetic
internal fun <T> buildJsonHttpDeserializer(
    deserializationStrategy: KSerializer<T>,
    deserializationJson: Json,
): HttpResponseDeserializer<T> {
    return Function { response ->
        if (!response.isSuccessful) {
            throw RequestFailedException(
                payload = response.body,
            )
        }
        val jsonRpcResponse =
            deserializationJson.decodeFromString(
                JsonRpcResponse.serializer(deserializationStrategy),
                response.body,
            )

        extractResult(jsonRpcResponse, response.body)
    }
}

internal fun <T> buildJsonBatchHttpDeserializer(
    deserializationStrategies: List<KSerializer<T>>,
    deserializationJson: Json,
): HttpResponseDeserializer<List<T>> {
    // TODO: In case of batch request, exception should not be thrown.
    // Instead, we want to return wrapped responses.
    // This enables access to successful results or encountered errors.

    return Function { response ->
        if (!response.isSuccessful) {
            throw RequestFailedException(
                payload = response.body,
            )
        }

        val jsonResponses = Json.parseToJsonElement(response.body).jsonArray
        val responses = jsonResponses.map {
            val deserializationStrategy = deserializationStrategies[it.jsonObject["id"]!!.jsonPrimitive.int]
            deserializationJson.decodeFromJsonElement(
                JsonRpcResponse.serializer(deserializationStrategy),
                it,
            )
        }

        val results = responses.sortedBy { it.id }.zip(jsonResponses)
            .map { (jsonRpcResponse, jsonResponse) ->
                extractResult(jsonRpcResponse, response.body, jsonResponse.toString())
            }
        results
    }
}
