package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import starknet.data.types.Response
import starknet.provider.http.HttpService
import java.util.concurrent.CompletableFuture

class HttpRequest<T: Response>(
    val url: String,
    val method: String,
    val headers: List<String>,
    val body: String,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    val deserializer: DeserializationStrategy<T>
): Request<T> {
    override fun send(): T {
        val response = HttpService.send(this)

        return Json.decodeFromString(deserializer, body)
    }

    override fun sendAsync(): CompletableFuture<T> {
        val future = HttpService.sendAsync(this)

        val newFuture = future.handle { response, e ->
            if (e != null) {
                throw e
            }

            Json.decodeFromString(deserializer, response)
        }

        return newFuture
    }
}