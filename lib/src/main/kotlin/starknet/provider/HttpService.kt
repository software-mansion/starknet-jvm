package starknet.provider

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.InputStream

val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

class HttpService(private val url: String, private val method: String) : Service() {
    private val client = OkHttpClient()

    override fun performIO(payload: String): InputStream {
        val requestBody = payload.toRequestBody(JSON_MEDIA_TYPE)

        // TODO - Add headers

        val request = okhttp3
            .Request
            .Builder()
            .url(url)
            .method(method, requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body

        if (response.isSuccessful) {
            return if (responseBody != null) {
                ByteArrayInputStream(responseBody.bytes())
            } else {
                // TODO: Should it be empty byte array or null returned from performIO method?
                ByteArrayInputStream(null)
            }
        } else {
            val code = response.code
            val text = response.body?.string() ?: "unknown"

            throw java.lang.Exception("Invalid response, code: $code, response: $text")
        }
    }
}