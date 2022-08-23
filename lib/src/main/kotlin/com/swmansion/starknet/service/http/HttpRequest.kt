package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture

internal class HttpRequest<T>(
    private val payload: HttpService.Payload,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    private val deserializer: DeserializationStrategy<T>,
) : Request<T> {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun send(): T {
        val response = HttpService.send(payload)

        return json.decodeFromString(deserializer, response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return HttpService.sendAsync(payload).thenApply { response ->
            Json.decodeFromString(deserializer, response)
        }
    }
}
