package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.serializers.JsonRpcErrorSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
