package com.swmansion.starknet.provider.rpc

import com.swmansion.starknet.data.types.StarknetResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class JsonRpcResponse<T : StarknetResponse>(
    @SerialName("id")
    val id: Int,

    @SerialName("jsonrpc")
    val jsonRpc: String,

    @SerialName("result")
    val result: T? = null,

    @SerialName("error")
    val error: JsonRpcError? = null,
)
