package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpRequestDeserializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

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

@Serializable
internal data class JsonRpcError(
    @SerialName("code")
    val code: Int,

    @SerialName("message")
    val message: String,
)

internal fun <T> buildJsonHttpDeserializer(deserializationStrategy: KSerializer<T>): HttpRequestDeserializer<T> {
    return { response ->
        if (!response.isSuccessful) {
            throw if (response.body == null) RequestFailedException("Request failed") else RequestFailedException(
                response.body,
            )
        }

        val jsonRpcResponse =
            Json.decodeFromString(
                JsonRpcResponse.serializer(deserializationStrategy),
                response.body!!,
            ) // Can we assume that if the request was successful, it will have a body?

        if (jsonRpcResponse.error != null) {
            throw RpcRequestFailedException(jsonRpcResponse.error.code, jsonRpcResponse.error.message)
        }

        if (jsonRpcResponse.result == null) {
            // FIXME add more specific error
            throw RequestFailedException(response.body)
        }

        jsonRpcResponse.result
    }
}
