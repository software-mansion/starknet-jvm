package com.swmansion.starknet.service.http.requests

import com.swmansion.starknet.data.types.BatchResult
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.rpc.JsonRpcRequest
import com.swmansion.starknet.provider.rpc.buildJsonHttpBatchDeserializerWithMultipleTypes
import com.swmansion.starknet.service.http.HttpService
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.CompletableFuture

class HttpBatchRequestWithMultipleTypes private constructor(
    private val payload: HttpService.Payload,
    private val deserializer: HttpResponseDeserializer<List<Result<BatchResult>>>,
    private val service: HttpService,
) : Request<List<Result<BatchResult>>> {

    constructor(
        url: String,
        jsonRpcRequests: List<JsonRpcRequest>,
        responseDeserializers: List<KSerializer<out BatchResult>>,
        deserializationJson: Json,
        service: HttpService,
    ) : this(
        HttpService.Payload(
            url = url,
            method = "POST",
            params = emptyList(),
            body = Json.encodeToString(jsonRpcRequests),
        ),
        deserializer = buildJsonHttpBatchDeserializerWithMultipleTypes(responseDeserializers, deserializationJson),
        service = service,
    )

    override fun send(): List<Result<BatchResult>> {
        val response = service.send(payload)
        return deserializer.apply(response)
    }

    override fun sendAsync(): CompletableFuture<List<Result<BatchResult>>> {
        return service.sendAsync(payload).thenApplyAsync(deserializer)
    }
}