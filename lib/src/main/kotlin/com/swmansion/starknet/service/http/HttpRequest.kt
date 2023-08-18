package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture
import java.util.function.Function

// Whole HttpResponse is required for deserialization instead of just
// body while gateway is supported, because gateway errors are returned
// as http errors with json body.
typealias HttpResponseDeserializer<T> = Function<HttpResponse, T>

class HttpRequest<T>(
    val payload: HttpService.Payload,
    val deserializer: HttpResponseDeserializer<T>,
    val service: HttpService,
) : Request<T> {

    override fun send(): T {
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}
