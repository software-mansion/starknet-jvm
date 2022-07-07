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
        val payload = HttpService.Payload(url, method, headers, body)
        val response = HttpService.send(payload)

        return Json.decodeFromString(deserializer, response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        val payload = HttpService.Payload(url, method, headers, body)
        val future = HttpService.sendAsync(payload)

        val newFuture = future.handle { response, e ->
            if (e != null) {
                throw e
            }

            Json.decodeFromString(deserializer, response)
        }

        return newFuture
    }
}