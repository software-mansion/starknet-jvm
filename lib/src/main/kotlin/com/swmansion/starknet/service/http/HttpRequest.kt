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

class HttpRequest<T> private constructor(
        val jsonRpcRequest: JsonRpcRequest,
        val serializer: KSerializer<T>,
        private val payload: HttpService.Payload,
        private val deserializer: HttpResponseDeserializer<T>,
        private val service: HttpService,
) : Request<T> {

    constructor(
            url: String,
            jsonRpcRequest: JsonRpcRequest,
            serializer: KSerializer<T>,
            deserializationJson: Json,
            service: HttpService,
    ) : this(
            jsonRpcRequest = jsonRpcRequest,
            serializer = serializer,
            payload = HttpService.Payload(
                    url,
                    "POST",
                    emptyList(),
                    Json.encodeToString(jsonRpcRequest),
            ),
            deserializer = buildJsonHttpDeserializer(serializer, deserializationJson),
            service = service,
    )

    override fun send(): T {
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}

class BatchHttpRequest<T> private constructor(
        private val payload: Lazy<HttpService.Payload>,
        private val deserializer: HttpResponseDeserializer<List<T>>,
        private val service: HttpService,
) : Request<List<T>> {

    constructor(
            url: String,
            jsonRpcRequests: List<JsonRpcRequest>,
            responseDeserializers: List<KSerializer<T>>,
            deserializationJson: Json,
            service: HttpService,
    ) : this(
            payload = lazy {
                HttpService.Payload(
                        url = url,
                        method = "POST",
                        params = emptyList(),
                        body = Json.encodeToString(jsonRpcRequests),
                )
            },
            deserializer = buildJsonBatchHttpDeserializer(responseDeserializers, deserializationJson),
            service = service,
    )

    override fun send(): List<T> {
        val response = service.send(payload.value)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<List<T>> {
        return service.sendAsync(payload.value).thenApplyAsync(deserializer)
    }
}

