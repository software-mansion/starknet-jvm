package com.swmansion.starknet.service.http

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.service.http.HttpService.Payload
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * Service for making http requests using OkHttp library. You can provide it with your client to
 * [avoid wasting resources](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared).
 * @param client OkHttpClient used for making requests
 */
class OkHttpService(private val client: OkHttpClient) : HttpService {
    constructor() : this(OkHttpClient())

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun buildRequest(payload: Payload): okhttp3.Request {
        val body = payload.body?.toRequestBody(JSON_MEDIA_TYPE)
        val url = buildRequestUrl(payload.url, payload.params)

        return okhttp3.Request.Builder().url(url).method(payload.method, body).build()
    }

    private fun buildRequestUrl(
        baseUrl: String,
        params: List<Pair<String, String>>,
    ): String {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()

        for (param in params) {
            urlBuilder.addQueryParameter(param.first, param.second)
        }

        return urlBuilder.build().toString()
    }

    private fun processHttpResponse(response: Response): HttpResponse {
        if (response.body == null) {
            throw RequestFailedException("HTTP request failed with code = ${response.code}", "")
        }
        return HttpResponse(response.isSuccessful, response.code, response.body!!.string())
    }

    /**
     * Send a synchronous http request.
     *
     * @param payload a payload to be sent
     */
    override fun send(payload: Payload): HttpResponse {
        val httpRequest = buildRequest(payload)

        val response = client.newCall(httpRequest).execute()

        return processHttpResponse(response)
    }

    /**
     * Send an asynchronous http request.
     *
     * @param payload a payload to be sent
     */
    override fun sendAsync(payload: Payload): CompletableFuture<HttpResponse> {
        val httpRequest = buildRequest(payload)

        val future = CompletableFuture<HttpResponse>()

        client.newCall(httpRequest).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(RequestFailedException(e.message ?: "Unknown HTTP error.", ""))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val parsedResponse = processHttpResponse(response)
                        future.complete(parsedResponse)
                    } catch (e: Exception) {
                        future.completeExceptionally(e)
                    }
                }
            },
        )

        return future
    }
}
