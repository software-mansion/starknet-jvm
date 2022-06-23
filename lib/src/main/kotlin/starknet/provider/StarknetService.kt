package starknet.provider

import starknet.data.types.Response
import java.util.concurrent.CompletableFuture

interface StarknetService {
    fun<T: Response> send(request: Request<T>): T

    fun<T: Response> sendAsync(request: Request<T>): CompletableFuture<T>
}