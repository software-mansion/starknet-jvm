package com.swmansion.starknet.service.http.requests

import com.swmansion.starknet.data.types.StarknetResponse
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.rpc.JsonRpcRequest
import com.swmansion.starknet.provider.rpc.buildJsonHttpDeserializer
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture
import java.util.function.Function

typealias HttpResponseDeserializer<T> = Function<HttpResponse, T>

class HttpRequest<T : StarknetResponse> private constructor(
    internal val jsonRpcRequest: JsonRpcRequest,
    internal val serializer: KSerializer<T>,
    private val payload: Lazy<HttpService.Payload>,
    private val deserializer: HttpResponseDeserializer<T>,
    private val service: HttpService,
) : Request<T> {

    internal constructor(
        url: String,
        jsonRpcRequest: JsonRpcRequest,
        serializer: KSerializer<T>,
        deserializationJson: Json,
        service: HttpService,
    ) : this(
        jsonRpcRequest = jsonRpcRequest,
        serializer = serializer,
        payload = lazy {
            HttpService.Payload(
                url,
                "POST",
                emptyList(),
                Json.encodeToString(jsonRpcRequest),
            )
        },
        deserializer = buildJsonHttpDeserializer(serializer, deserializationJson),
        service = service,
    )

    override fun send(): T {
        val response = service.send(payload.value)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<T> {
        return service.sendAsync(payload.value).thenApplyAsync(deserializer)
    }
}
