package starknet.provider.http

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import starknet.data.types.Response
import starknet.provider.Request
import java.util.concurrent.CompletableFuture

class HttpRequest<T>(
    private val payload: HttpService.Payload,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    private val deserializer: DeserializationStrategy<T>
): Request<T> {
    override fun send(): T {
        val response = HttpService.send(payload)

        return Json.decodeFromString(deserializer, response)
    }

    override fun sendAsync(): CompletableFuture<T> {
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