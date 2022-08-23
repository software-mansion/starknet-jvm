package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.service.RequestProcessingService
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture

internal class HttpRequest<T>(
    private val payload: RequestProcessingService.Payload,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    private val deserializer: DeserializationStrategy<T>,
    private val service: RequestProcessingService,
) : Request<T> {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun send(): T {
        val response = service.send(payload)

        return json.decodeFromString(deserializer, response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload).thenApply { response ->
            Json.decodeFromString(deserializer, response)
        }
    }
}
