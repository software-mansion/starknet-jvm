package com.swmansion.starknet.provider.rpc

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class JsonRpcResponse<T>(
    @SerialName("id")
    val id: Int,

    @SerialName("jsonrpc")
    val jsonRpc: String,

    @SerialName("result")
    val result: T? = null,

    @SerialName("error")
    val error: JsonElement? = null, // FIXME: Add error types
)

internal class JsonRpcResponseDeserializer<T>(private val dataSerializer: KSerializer<T>) : DeserializationStrategy<T> {
    override val descriptor: SerialDescriptor
        get() = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): T {
        require(decoder is JsonDecoder)

        val responseJson = decoder.decodeJsonElement()
        val format = Json { ignoreUnknownKeys = true }
        val response = format.decodeFromJsonElement(JsonRpcResponse.serializer(dataSerializer), responseJson)

        if (response.result != null) {
            return response.result
        } else if (response.error != null) {
            // FIXME: Add custom exception
            throw Exception("Error")
        } else {
            throw Exception("Error")
        }
    }
}
