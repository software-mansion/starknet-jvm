package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.JsonRpcErrorPolymorphicSerializer
import com.swmansion.starknet.data.serializers.JsonRpcStandardErrorSerializer
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
    @Serializable(with = JsonRpcErrorPolymorphicSerializer::class)
    val error: JsonRpcError? = null,
)

internal sealed class JsonRpcError {
    @SerialName("code")
    abstract val code: Int

    @SerialName("message")
    abstract val message: String

    @SerialName("data")
    abstract val data: Any?
}

@Serializable(with = JsonRpcStandardErrorSerializer::class)
internal data class JsonRpcStandardError(
    @SerialName("code")
    override val code: Int,

    @SerialName("message")
    override val message: String,

    @SerialName("data")
    override val data: String? = null,
) : JsonRpcError()

@Serializable
internal data class JsonRpcContractError(
    @SerialName("code")
    override val code: Int,

    @SerialName("message")
    override val message: String,

    @SerialName("data")
    override val data: JsonRpcContractErrorData,
) : JsonRpcError()

@Serializable
internal data class JsonRpcContractErrorData(
    @SerialName("revert_error")
    val revertError: String,
) {
    override fun toString(): String {
        return revertError
    }
}

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
                data = jsonRpcResponse.error.data?.toString(),
                payload = response.body,
            )
        }

        if (jsonRpcResponse.result == null) {
            throw RequestFailedException(message = "Response did not contain a result", payload = response.body)
        }

        jsonRpcResponse.result
    }
}
