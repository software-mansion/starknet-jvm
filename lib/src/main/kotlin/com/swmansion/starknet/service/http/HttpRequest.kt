package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture
import java.util.function.Function

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
