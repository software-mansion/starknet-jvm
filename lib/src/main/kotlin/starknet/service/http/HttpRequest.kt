package starknet.service.http

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
        return HttpService.sendAsync(payload).thenApply { response ->
            Json.decodeFromString(deserializer, response)
        }
    }
}