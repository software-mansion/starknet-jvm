package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.requests.HttpResponseDeserializer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.function.Function

private fun <T> extractResult(jsonRpcResponse: JsonRpcResponse<T>, fullPayload: String, payload: String): Result<T> {
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

private fun validateResponseSuccess(response: HttpResponse) {
    if (!response.isSuccessful) {
        throw RequestFailedException(
            payload = response.body,
        )
    }
}

@JvmSynthetic
internal fun <T> buildJsonHttpDeserializer(
    deserializationStrategy: KSerializer<T>,
    deserializationJson: Json,
): HttpResponseDeserializer<T> {
    return Function { response ->
        validateResponseSuccess(response)

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
): HttpResponseDeserializer<List<Result<T>>> {
    return Function { response ->
        validateResponseSuccess(response)

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

internal fun <T> buildJsonHttpBatchDeserializerOfDifferentTypes(
    deserializationStrategies: List<KSerializer<out T>>,
    deserializationJson: Json,
): HttpResponseDeserializer<List<Result<T>>> {
    return Function { response ->
        validateResponseSuccess(response)

        val jsonResponses = Json.parseToJsonElement(response.body).jsonArray
        val responses = jsonResponses.map {
            val deserializationStrategy = deserializationStrategies[it.jsonObject["id"]!!.jsonPrimitive.int]
            deserializationJson.decodeFromJsonElement(
                JsonRpcResponse.serializer(deserializationStrategy),
                it,
            )
        }

        val results = responses.zip(jsonResponses)
            .map { (jsonRpcResponse, jsonResponse) ->
                extractResult(jsonRpcResponse, response.body, jsonResponse.toString())
            }
        results
    }
}
