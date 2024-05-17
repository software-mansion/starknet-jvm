package com.swmansion.starknet.service.http.requests

import com.swmansion.starknet.data.types.HttpRequestType
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.rpc.JsonRpcRequest
import com.swmansion.starknet.provider.rpc.buildJsonHttpBatchDeserializer
import com.swmansion.starknet.provider.rpc.buildJsonHttpBatchDeserializerOfDifferentTypes
import com.swmansion.starknet.service.http.HttpService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture

class HttpBatchRequest<T : HttpRequestType> private constructor(
    private val payload: HttpService.Payload,
    private val deserializer: HttpResponseDeserializer<List<Result<T>>>,
    private val service: HttpService,
) : Request<List<Result<T>>> {
    override fun send(): List<Result<T>> {
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<List<Result<T>>> {
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }

    companion object {
        @JvmStatic
        fun <T : HttpRequestType>fromRequests(
            url: String,
            jsonRpcRequests: List<JsonRpcRequest>,
            responseDeserializers: List<KSerializer<T>>,
            deserializationJson: Json,
            service: HttpService,
        ): HttpBatchRequest<T> {
            return HttpBatchRequest(
                HttpService.Payload(
                    url = url,
                    method = "POST",
                    params = emptyList(),
                    body = Json.encodeToString(jsonRpcRequests),
                ),
                deserializer = buildJsonHttpBatchDeserializer(responseDeserializers, deserializationJson),
                service = service,
            )
        }

        @JvmStatic
        fun <T : HttpRequestType>fromRequestsAny(
            url: String,
            jsonRpcRequests: List<JsonRpcRequest>,
            responseDeserializers: List<KSerializer<out T>>,
            deserializationJson: Json,
            service: HttpService,
        ): HttpBatchRequest<T> {
            return HttpBatchRequest(
                HttpService.Payload(
                    url = url,
                    method = "POST",
                    params = emptyList(),
                    body = Json.encodeToString(jsonRpcRequests),
                ),
                deserializer = buildJsonHttpBatchDeserializerOfDifferentTypes(responseDeserializers, deserializationJson),
                service = service,
            )
        }
    }
}
