package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import starknet.data.types.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture

val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class HttpService(private val url: String, private val method: String) : Service {
    private val client = OkHttpClient()

    private fun buildHttpRequest(payload: String): okhttp3.Request {
        val requestBody = payload.toRequestBody(JSON_MEDIA_TYPE)

        return okhttp3
            .Request
            .Builder()
            .url(url)
            .method(method, requestBody)
            .build()
    }

    private fun<T: Response> processHttpResponse(response: okhttp3.Response, deserializer: DeserializationStrategy<T>): T {
        val responseBody = response.body

        if (response.isSuccessful) {
            if (responseBody != null) {
                return Json.decodeFromString(deserializer, responseBody.toString())
            } else {
                // TODO: Create custom exception
                throw java.lang.Exception("Empty response body")
            }
        } else {
            val code = response.code
            val text = response.body?.string() ?: "unknown"

            // TODO: Create custom exception
            throw java.lang.Exception("Invalid response, code: $code, response: $text")
        }
    }

    override fun <T : Response> send(request: Request<T>): T {
        val httpRequest = buildHttpRequest(request.payload)

        val response = client.newCall(httpRequest).execute()

        return processHttpResponse(response, request.deserializer)
    }

    override fun <T : Response> sendAsync(request: Request<T>): CompletableFuture<T> {
        val httpRequest = buildHttpRequest(request.payload)

        val future = CompletableFuture<T>()

        client.newCall(httpRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                try {
                    val parsedResponse = processHttpResponse(response, request.deserializer)
                    future.complete(parsedResponse)
                } catch(e: Exception) {
                    future.completeExceptionally(e)
                }
            }
        })

        return future
    }
}