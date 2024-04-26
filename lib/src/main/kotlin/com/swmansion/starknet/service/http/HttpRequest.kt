package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.rpc.JsonRpcRequest
import com.swmansion.starknet.provider.rpc.buildJsonBatchHttpDeserializer
import com.swmansion.starknet.provider.rpc.buildJsonHttpDeserializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
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

    override fun send(): T {
        val deserializer = buildJsonHttpDeserializer(serializer, deserializationJson)
        val payload =
                HttpService.Payload(url, "POST", emptyList(), jsonRpcRequest.toString())
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        val deserializer = buildJsonHttpDeserializer(serializer, deserializationJson)
        val payload =
                HttpService.Payload(url, "POST", emptyList(), jsonRpcRequest.toString())
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}

class BatchHttpRequest<T>(
        val url: String,
        val jsonRpcRequests: List<JsonRpcRequest>,
        val responseSerializers: List<KSerializer<T>>,
        val deserializationJson: Json,
        val service: HttpService,
) : Request<List<T>> {
    private fun parseResponse(response: HttpResponse): List<T> {
        val results = buildJsonBatchHttpDeserializer<T>(responseSerializers, deserializationJson).apply(response)
        return results
    }

    private fun getPayload(): HttpService.Payload {
        val body = jsonRpcRequests.map { Json.encodeToString(JsonRpcRequest.serializer(), it) }.toString()
        val payload = HttpService.Payload(
                url,
                "POST",
                emptyList(),
                body,
        )

        return payload
    }

    override fun send(): List<T> {
        val payload = getPayload()
        val response = service.send(payload)
        return parseResponse(response)
    }

    override fun sendAsync(): CompletableFuture<List<T>> {
        val payload = getPayload()
        return service.sendAsync(payload).thenApplyAsync(this::parseResponse)
    }
}
