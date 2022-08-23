package com.swmansion.starknet.service.http.handlers

import com.swmansion.starknet.provider.exceptions.GatewayRequestFailedException
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class BasicHttpErrorHandler : HttpErrorHandler {
    @Serializable
    private data class GatewayError(
        @SerialName("status_code")
        val code: Int,

        @SerialName("message")
        val message: String,
    )

    override fun handle(response: String): Nothing = try {
        val gatewayError = Json.decodeFromString(GatewayError.serializer(), response)
        throw GatewayRequestFailedException(gatewayError.message)
    } catch (e: SerializationException) {
        throw RequestFailedException(response)
    }
}
