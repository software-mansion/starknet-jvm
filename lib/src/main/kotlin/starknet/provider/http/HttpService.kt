package starknet.provider.http

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import starknet.data.types.Response
import starknet.provider.HttpRequest
import java.io.IOException
import java.util.concurrent.CompletableFuture

val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class HttpService {

    class HttpServiceFailedResponse(message: String, val code: Int, val response: String) : Exception(message)

    companion object {
        private fun <T : Response> buildNetworkRequest(request: HttpRequest<T>): okhttp3.Request {
            val requestBody = request.body.toRequestBody(JSON_MEDIA_TYPE)

            return okhttp3
                .Request
                .Builder()
                .url(request.url)
                .method(request.method, requestBody)
                .build()
        }

        private fun processHttpResponse(response: okhttp3.Response): String {
            val responseBody = response.body

            if (response.isSuccessful) {
                return responseBody.toString()
            } else {
                val code = response.code
                val text = response.body?.string() ?: "unknown"

                throw HttpServiceFailedResponse("Invalid response, code: $code, response: $text", code, text)
            }
        }

        fun <T : Response> send(request: HttpRequest<T>): String {
            val client = OkHttpClient()
            val httpRequest = buildNetworkRequest(request)

            val response = client.newCall(httpRequest).execute()

            return processHttpResponse(response)
        }

        fun <T : Response> sendAsync(request: HttpRequest<T>): CompletableFuture<String> {
            val client = OkHttpClient()
            val httpRequest = buildNetworkRequest(request)

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