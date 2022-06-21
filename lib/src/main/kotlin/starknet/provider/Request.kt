package starknet.provider

import java.util.concurrent.CompletableFuture

class Request<T>(
    val service: StarknetService,
    val payload: String
) {
    fun send(): T {
        return service.send(this)
    }
    fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(this)
    }
}