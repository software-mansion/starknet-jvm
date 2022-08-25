package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture

internal typealias HttpRequestDeserializer<T> = (HttpResponse) -> T

internal class HttpRequest<T>(
    private val payload: HttpService.Payload,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    private val deserialize: HttpRequestDeserializer<T>,
    private val service: HttpService,
) : Request<T> {

    override fun send(): T {
        val response = service.send(payload)

        return deserialize(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload).thenApply(deserialize)
    }
}
