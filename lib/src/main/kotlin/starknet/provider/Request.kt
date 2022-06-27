package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import starknet.data.types.Response
import starknet.provider.service.HttpService
import java.util.concurrent.CompletableFuture

class Request<T: Response>(
    val url: String,
    val method: String,
    val headers: List<String>,
    val body: String,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    val deserializer: DeserializationStrategy<T>
) {
    fun send(): T {
        return HttpService.send(this)
    }

    fun sendAsync(): CompletableFuture<T> {
        return HttpService.sendAsync(this)
    }
}