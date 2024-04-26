package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.JsonRpcErrorSerializer
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpResponseDeserializer
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.util.function.Function

@Serializable
internal data class JsonRpcResponse<T>(
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

internal fun <T> extractResult(jsonRpcResponse: JsonRpcResponse<T>, response: HttpResponse): T {
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

        extractResult(jsonRpcResponse, response)
    }
}

internal fun <T> buildJsonBatchHttpDeserializer(
    deserializationStrategies: List<KSerializer<T>>,
    deserializationJson: Json,
): HttpResponseDeserializer<List<T>> {
    // TODO: In case of batch request, exception should not be thrown.
    // Instead, we want to gather results and provide them with a wrapper.
    // This way could access the results of the successful/error requests.

    return Function { response ->
        if (!response.isSuccessful) {
            throw RequestFailedException(
                payload = response.body,
            )
        }

        val jsonArray = Json.parseToJsonElement(response.body).jsonArray
        val jsonRpcResponses = deserializationStrategies.mapIndexed { index, strategy ->
            deserializationJson.decodeFromString(
                JsonRpcResponse.serializer(strategy),
                jsonArray[index].toString(),
            )
        }

        // The Response objects being returned from a batch call may be returned in any order within the array, so
        // we need to sort the responses by id to match the order of the requests.
        val results = jsonRpcResponses.sortedBy { it.id }.map { extractResult(it, response) }

        results
    }
}
