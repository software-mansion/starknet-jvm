package com.swmansion.starknet.service.http

import kotlinx.serialization.json.JsonObject
import java.util.concurrent.CompletableFuture

/**
 * Data class representing a completed http request.
 *
 * @param isSuccessful indicates if result completed with code 2xx
 * @param code http return code
 * @param body http return body
 */
data class HttpResponse(
    val isSuccessful: Boolean,
    val code: Int,
    val body: String,
)

/**
 * Service for http communication.
 *
 * Implementers of this interface provide methods to facilitate http communication between Providers and StarkNet
 */
interface HttpService {
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

        constructor(url: String, method: String) : this(url, method, emptyList(), null)
    }

    /**
     * Send a synchronous http request.
     *
     * @param payload a payload to be sent
     */
    fun send(payload: Payload): HttpResponse

    /**
     * Send an asynchronous http request.
     *
     * @param payload a payload to be sent
     */
    fun sendAsync(payload: Payload): CompletableFuture<HttpResponse>
}
