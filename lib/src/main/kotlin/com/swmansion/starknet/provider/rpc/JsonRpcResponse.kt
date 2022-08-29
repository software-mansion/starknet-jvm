package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpResponseDeserializer
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

internal fun <T> buildJsonHttpDeserializer(deserializationStrategy: KSerializer<T>): HttpResponseDeserializer<T> {
    return { response ->
        if (!response.isSuccessful) {
            throw RequestFailedException(
                response.body,
            )
        }

        val jsonRpcResponse =
            Json.decodeFromString(
                JsonRpcResponse.serializer(deserializationStrategy),
                response.body,
            )

        if (jsonRpcResponse.error != null) {
            throw RpcRequestFailedException(jsonRpcResponse.error.code, jsonRpcResponse.error.message)
        }

        if (jsonRpcResponse.result == null) {
            throw RequestFailedException("Response did not contain a result")
        }

        jsonRpcResponse.result
    }
}
