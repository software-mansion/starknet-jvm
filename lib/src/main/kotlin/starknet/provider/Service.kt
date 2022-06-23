package starknet.provider

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import starknet.data.types.CallContractResponse
import starknet.data.types.Response
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

abstract class Service : StarknetService {
    protected abstract fun performIO(payload: String): InputStream

    override fun <T: Response> send(request: Request<T>): T {
        val inputStream = performIO(request.payload)

        return Json.decodeFromString(request.deserializer, inputStream.toString())
    }

    override fun <T: Response> sendAsync(request: Request<T>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync {
            send(request)
        }
    }
}