package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import starknet.data.types.Response
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

class Request<T: Response>(
    private val service: StarknetService,
    val payload: String,
    val deserializer: DeserializationStrategy<T>
) {
    fun send(): T {
        return service.send(this)
    }

    fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(this)
    }
}