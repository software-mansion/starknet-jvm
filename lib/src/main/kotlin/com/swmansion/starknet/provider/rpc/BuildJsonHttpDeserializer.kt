package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.service.http.HttpResponseDeserializer
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
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

@Serializable
private data class JsonRpcError(
    @SerialName("code")
    val code: Int,

    @SerialName("message")
    val message: String,

    @SerialName("data")
    val data: JsonRpcErrorData? = null,
)

@Serializable
private data class JsonRpcErrorData(
    @SerialName("revert_error")
    val revertError: String,
)

@JvmSynthetic
internal fun <T> buildJsonHttpDeserializer(deserializationStrategy: KSerializer<T>): HttpResponseDeserializer<T> {
    return Function { response ->
        if (!response.isSuccessful) {
            throw RequestFailedException(
                payload = response.body,
            )
        }

        val jsonRpcResponse =
            Json.decodeFromString(
                JsonRpcResponse.serializer(deserializationStrategy),
                response.body,
            )

        if (jsonRpcResponse.error != null) {
            throw RpcRequestFailedException(
                code = jsonRpcResponse.error.code,
                message = jsonRpcResponse.error.message,
                revertError = jsonRpcResponse.error.data?.revertError,
                payload = response.body,
            )
        }

        if (jsonRpcResponse.result == null) {
            throw RequestFailedException(message = "Response did not contain a result", payload = response.body)
        }

        jsonRpcResponse.result
    }
}
