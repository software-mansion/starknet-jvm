package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.requests.HttpResponseDeserializer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.function.Function

private fun <T> getResult(jsonRpcResponse: JsonRpcResponse<T>, fullPayload: String, payload: String): Result<T> {
    if (jsonRpcResponse.error != null) {
        return Result.failure(
            RpcRequestFailedException(
                code = jsonRpcResponse.error.code,
                message = jsonRpcResponse.error.message,
                data = jsonRpcResponse.error.data,
                payload = payload,
            ),
        )
    }

    if (jsonRpcResponse.result == null) {
        return Result.failure(
            RequestFailedException(
                message = "Response did not contain a result",
                payload = fullPayload,
            ),
        )
    }
    return Result.success(jsonRpcResponse.result)
}

private fun assertResponseSuccess(response: HttpResponse) {
    if (!response.isSuccessful) {
        throw RequestFailedException(
            payload = response.body,
        )
    }
}

private fun <T> getOrderedRpcResults(
    response: HttpResponse,
    deserializationStrategies: List<KSerializer<out T>>,
    deserializationJson: Json,
): List<Result<T>> {
    assertResponseSuccess(response)

    val jsonResponses = Json.parseToJsonElement(response.body).jsonArray
    val orderedResults = MutableList<Result<T>?>(jsonResponses.size) { null }

    jsonResponses.forEach { jsonElement ->
        val id = jsonElement.jsonObject["id"]!!.jsonPrimitive.int
        val deserializationStrategy = deserializationStrategies[id]
        val jsonRpcResponse = deserializationJson.decodeFromJsonElement(
            JsonRpcResponse.serializer(deserializationStrategy),
            jsonElement,
        )
        orderedResults[id] = getResult(jsonRpcResponse, response.body, jsonElement.toString())
    }

    return orderedResults.filterNotNull()
}

@JvmSynthetic
internal fun <T> buildJsonHttpDeserializer(
    deserializationStrategy: KSerializer<T>,
    deserializationJson: Json,
): HttpResponseDeserializer<T> {
    return Function { response ->
        assertResponseSuccess(response)

        val jsonRpcResponse =
            deserializationJson.decodeFromString(
                JsonRpcResponse.serializer(deserializationStrategy),
                response.body,
            )

        if (jsonRpcResponse.error != null) {
            throw RpcRequestFailedException(
                code = jsonRpcResponse.error.code,
                message = jsonRpcResponse.error.message,
                data = jsonRpcResponse.error.data,
                payload = response.body,
            )
        }

        if (jsonRpcResponse.result == null) {
            throw RequestFailedException(message = "Response did not contain a result", payload = response.body)
        }
        jsonRpcResponse.result
    }
}

internal fun <T> buildJsonHttpBatchDeserializer(
    deserializationStrategies: List<KSerializer<T>>,
    deserializationJson: Json,
): HttpResponseDeserializer<List<Result<T>>> = Function { response ->
    getOrderedRpcResults(response, deserializationStrategies, deserializationJson)
}

internal fun <T> buildJsonHttpBatchDeserializerOfDifferentTypes(
    deserializationStrategies: List<KSerializer<out T>>,
    deserializationJson: Json,
): HttpResponseDeserializer<List<Result<T>>> = Function { response ->
    getOrderedRpcResults(response, deserializationStrategies, deserializationJson)
}
