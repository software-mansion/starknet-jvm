package starknet.service.http

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.CompletableFuture

class HttpService {
    data class Payload(val url: String, val method: String, val params: List<String>, val body: String?)

    class HttpServiceFailedResponse(message: String, val code: Int, val response: String) : Exception(message)

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun buildRequest(payload: Payload): okhttp3.Request {
            val requestBody = payload.body?.toRequestBody(JSON_MEDIA_TYPE)

            return okhttp3
                .Request
                .Builder()
                .url(payload.url)
                .method(payload.method, requestBody)
                .build()
        }

        private fun processHttpResponse(response: okhttp3.Response): String {
            val responseBody = response.body

            if (response.isSuccessful) {
                return responseBody?.string() ?: ""
            } else {
                val code = response.code
                val text = response.body?.string() ?: "unknown"

                throw HttpServiceFailedResponse("Invalid response, code: $code, response: $text", code, text)
            }
        }

        @JvmStatic
        fun send(payload: Payload): String {
            val client = OkHttpClient()
            val httpRequest = buildRequest(payload)

            val response = client.newCall(httpRequest).execute()

            return processHttpResponse(response)
        }

        @JvmStatic
        fun sendAsync(payload: Payload): CompletableFuture<String> {
            val client = OkHttpClient()
            val httpRequest = buildRequest(payload)

            val future = CompletableFuture<String>()

            client.newCall(httpRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
                }

                override fun onResponse(call: Call, response: okhttp3.Response) {
                    try {
                        val parsedResponse = processHttpResponse(response)
                        future.complete(parsedResponse)
                    } catch (e: Exception) {
                        future.completeExceptionally(e)
                    }
                }
            })

            return future
        }
    }

}