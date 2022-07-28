package starknet.service.http

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import starknet.provider.Request
import java.util.concurrent.CompletableFuture

class HttpRequest<T>(
    private val payload: HttpService.Payload,
    // TODO: Probably it could be abstracted, to not depend on kotlinx serialization
    private val deserializer: DeserializationStrategy<T>
) : Request<T> {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun send(): T {
        val response = HttpService.send(payload)

        return json.decodeFromString(deserializer, response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return HttpService.sendAsync(payload).thenApply { response ->
            Json.decodeFromString(deserializer, response)
        }
    }

    companion object {
        fun <T> makeRequest(
            url: String,
            method: String,
            body: String,
            deserializer: DeserializationStrategy<T>
        ): HttpRequest<T> {
            val httpPayload = HttpService.Payload(url, method, body)
            return HttpRequest(httpPayload, deserializer)
        }

        fun <T> makeRequest(
            url: String,
            method: String,
            params: List<Pair<String, String>>,
            deserializer: DeserializationStrategy<T>
        ): HttpRequest<T> {
            val httpPayload = HttpService.Payload(url, method, params)
            return HttpRequest(httpPayload, deserializer)
        }

        fun <T> makeRequest(
            url: String,
            method: String,
            params: List<Pair<String, String>>,
            body: String,
            deserializer: DeserializationStrategy<T>
        ): HttpRequest<T> {
            val httpPayload = HttpService.Payload(url, method, params, body)
            return HttpRequest(httpPayload, deserializer)
        }
    }
}
