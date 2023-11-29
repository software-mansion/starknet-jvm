package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture
import java.util.function.Function

// Gateway errors were returned as http errors with json body.
// Now that gateway is removed, this can be refactored to only use the response body instead of the whole HttpResponse.
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
