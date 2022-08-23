package com.swmansion.starknet.service

import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CompletableFuture

internal interface RequestProcessingService {
    data class Payload(
        val url: String,
        val method: String,
        val params: List<Pair<String, String>>,
        val body: String?,
    ) {
        constructor(
            url: String,
            method: String,
            params: List<Pair<String, String>>,
        ) : this(url, method, params, null)

        constructor(url: String, method: String, body: JsonObject) : this(
            url,
            method,
            emptyList(),
            body.toString(),
        )

        constructor(
            url: String,
            method: String,
            params: List<Pair<String, String>>,
            body: JsonObject,
        ) : this(
            url,
            method,
            params,
            body.toString(),
        )
    }

    /**
     * Send a synchronous http request.
     *
     * @param payload a payload to be sent
     */
    fun send(payload: Payload): String

    /**
     * Send an asynchronous http request.
     *
     * @param payload a payload to be sent
     */
    fun sendAsync(payload: Payload): CompletableFuture<String>
}
