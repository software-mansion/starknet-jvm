package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.rpc.JsonRpcRequest
import com.swmansion.starknet.provider.rpc.buildJsonBatchHttpDeserializer
import com.swmansion.starknet.provider.rpc.buildJsonHttpDeserializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture
import java.util.function.Function

typealias HttpResponseDeserializer<T> = Function<HttpResponse, T>

class HttpRequest<T>(
    val url: String,
    val jsonRpcRequest: JsonRpcRequest,
    val serializer: KSerializer<T>,
    val deserializationJson: Json,
    val service: HttpService,
) : Request<T> {

    private val payload: HttpService.Payload by lazy {
        val body = Json.encodeToString(jsonRpcRequest)
        val payload = HttpService.Payload(
            url,
            "POST",
            emptyList(),
            body,
        )
        payload
    }

    override fun send(): T {
        val deserializer = buildJsonHttpDeserializer(serializer, deserializationJson)
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        val deserializer = buildJsonHttpDeserializer(serializer, deserializationJson)
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}

class BatchHttpRequest<T>(
    val url: String,
    val jsonRpcRequests: List<JsonRpcRequest>,
    val responseDeserializers: List<KSerializer<T>>,
    val deserializationJson: Json,
    val service: HttpService,
) : Request<List<T>> {
    private fun parseResponse(response: HttpResponse): List<T> {
        val results = buildJsonBatchHttpDeserializer<T>(responseDeserializers, deserializationJson).apply(response)
        return results
    }

    private val payload: HttpService.Payload by lazy {
        HttpService.Payload(
            url = url,
            method = "POST",
            params = emptyList(),
            body = Json.encodeToString(jsonRpcRequests),
        )
    }

    override fun send(): List<T> {
        val response = service.send(payload)
        return parseResponse(response)
    }

    override fun sendAsync(): CompletableFuture<List<T>> {
        return service.sendAsync(payload).thenApplyAsync(this::parseResponse)
    }
}
