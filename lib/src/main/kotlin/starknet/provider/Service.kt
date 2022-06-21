package starknet.provider

import java.io.InputStream
import java.util.concurrent.CompletableFuture

abstract class Service : StarknetService {
    protected abstract fun performIO(payload: String): InputStream

    override fun <T> send(request: Request<T>): T {
        TODO("Not yet implemented")
        val inputStream = performIO(request.payload)

        if (inputStream != null) {
            // TODO: Deserialize
        }
    }

    override fun <T> sendAsync(request: Request<T>): CompletableFuture<T> {
        TODO("Not yet implemented")
    }
}