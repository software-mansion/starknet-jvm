package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.util.concurrent.CompletableFuture
import java.util.function.Function

typealias HttpResponseDeserializer<T> = Function<HttpResponse, T>


class HttpRequest<T>(
        val payload: HttpService.Payload,
        val deserializer: HttpResponseDeserializer<T>,
        val service: HttpService,
) : Request<T> {

    override fun send(): T {
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}


class BatchHttpRequest<T>(
        val payload: HttpService.Payload,
        val deserializer: HttpResponseDeserializer<T>,
        val service: HttpService,
) : Request<List<T>> {
    private fun parseResponse(response: HttpResponse): List<T> {
        val responseList = Json.parseToJsonElement(response.body).jsonArray
        return responseList.map { responseJson ->
            val deserializedValue = deserializer.apply(HttpResponse(response.isSuccessful, response.code, responseJson.toString()))
            deserializedValue
        }
    }

    override fun send(): List<T> {
        val response = service.send(payload)
        return parseResponse(response)
    }

    override fun sendAsync(): CompletableFuture<List<T>> {
        return service.sendAsync(payload).thenApplyAsync(this::parseResponse)
    }
}
