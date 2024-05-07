package com.swmansion.starknet.service.http.requests

import com.swmansion.starknet.data.types.HttpBatchRequestType
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.rpc.JsonRpcRequest
import com.swmansion.starknet.provider.rpc.buildJsonHttpBatchDeserializerOfDifferentTypes
import com.swmansion.starknet.service.http.HttpService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture

class HttpBatchRequestOfDifferentTypes private constructor(
    private val payload: HttpService.Payload,
    private val deserializer: HttpResponseDeserializer<List<Result<HttpBatchRequestType>>>,
    private val service: HttpService,
) : Request<List<Result<HttpBatchRequestType>>> {

    constructor(
        url: String,
        jsonRpcRequests: List<JsonRpcRequest>,
        responseDeserializers: List<KSerializer<out HttpBatchRequestType>>,
        deserializationJson: Json,
        service: HttpService,
    ) : this(
        HttpService.Payload(
            url = url,
            method = "POST",
            params = emptyList(),
            body = Json.encodeToString(jsonRpcRequests),
        ),
        deserializer = buildJsonHttpBatchDeserializerOfDifferentTypes(responseDeserializers, deserializationJson),
        service = service,
    )

    override fun send(): List<Result<HttpBatchRequestType>> {
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<List<Result<HttpBatchRequestType>>> {
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}
