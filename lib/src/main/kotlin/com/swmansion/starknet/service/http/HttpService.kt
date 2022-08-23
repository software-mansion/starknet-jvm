package com.swmansion.starknet.service.http

import com.swmansion.starknet.service.RequestProcessingService
import com.swmansion.starknet.service.RequestProcessingService.Payload
import com.swmansion.starknet.service.http.handlers.HttpErrorHandler
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
 * Service for making http requests.
 */
internal class HttpService(private val errorHandler: HttpErrorHandler) : RequestProcessingService {

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

    private fun processHttpResponse(response: Response): String {
        val responseBody = response.body

        if (response.isSuccessful) {
            return responseBody?.string() ?: ""
        } else {
            errorHandler.handle(response.body?.string() ?: "Unknown error")
        }
    }

    /**
     * Send a synchronous http request.
     *
     * @param payload a payload to be sent
     */
    override fun send(payload: Payload): String {
        val client = OkHttpClient()
        val httpRequest = buildRequest(payload)

        val response = client.newCall(httpRequest).execute()

        return processHttpResponse(response)
    }

    /**
     * Send an asynchronous http request.
     *
     * @param payload a payload to be sent
     */
    override fun sendAsync(payload: Payload): CompletableFuture<String> {
        val client = OkHttpClient()
        val httpRequest = buildRequest(payload)

        val future = CompletableFuture<String>()

        client.newCall(httpRequest).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
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
