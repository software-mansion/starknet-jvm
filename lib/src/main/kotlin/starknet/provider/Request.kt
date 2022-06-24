package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import starknet.data.types.Response
import starknet.provider.service.Service
import java.util.concurrent.CompletableFuture

class Request<T: Response>(
    private val service: Service,
    val payload: String,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    val deserializer: DeserializationStrategy<T>
) {
    fun send(): T {
        return service.send(this)
    }

    fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(this)
    }
}