package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture

// Whole HttpResponse is required for deserialization instead of just
// body while gateway is supported, because gateway errors are returned
// as http errors with json body.
internal typealias HttpResponseDeserializer<T> = (HttpResponse) -> T

internal class HttpRequest<T>(
    private val payload: HttpService.Payload,
    private val deserialize: HttpResponseDeserializer<T>,
    private val service: HttpService,
) : Request<T> {

    override fun send(): T {
        val response = service.send(payload)

        return deserialize(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload).thenApplyAsync(deserialize)
    }
}
