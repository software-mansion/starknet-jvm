package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import starknet.data.types.Response
import starknet.provider.Request
import java.io.IOException
import java.util.concurrent.CompletableFuture

val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class HttpService {

    class HttpServiceFailedResponse(message: String, val code: Int, val response: String) : Exception(message)
    class HttpServiceEmptyResponse() : Exception("Empty response body")

    companion object {
        private fun<T:Response> buildHttpRequest(request: Request<T>): okhttp3.Request {
            val requestBody = request.body.toRequestBody(JSON_MEDIA_TYPE)

            return okhttp3
                .Request
                .Builder()
                .url(request.url)
                .method(request.method, requestBody)
                .build()
        }

        private fun<T: Response> processHttpResponse(response: okhttp3.Response, deserializer: DeserializationStrategy<T>): T {
            val responseBody = response.body

            if (response.isSuccessful) {
                if (responseBody != null) {
                    return Json.decodeFromString(deserializer, responseBody.toString())
                } else {
                    throw HttpServiceEmptyResponse()
                }
            } else {
                val code = response.code
                val text = response.body?.string() ?: "unknown"

                throw HttpServiceFailedResponse("Invalid response, code: $code, response: $text", code, text)
            }
        }

        fun <T : Response> send(request: Request<T>): T {
            val client = OkHttpClient()
            val httpRequest = buildHttpRequest(request)

            val response = client.newCall(httpRequest).execute()

            return processHttpResponse(response, request.deserializer)
        }

        fun <T : Response> sendAsync(request: Request<T>): CompletableFuture<T> {
            val client = OkHttpClient()
            val httpRequest = buildHttpRequest(request)

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

}