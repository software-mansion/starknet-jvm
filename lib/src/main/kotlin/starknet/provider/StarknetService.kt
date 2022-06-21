package starknet.provider

import java.util.concurrent.CompletableFuture

interface StarknetService {
    fun<T> send(request: Request<T>): T

    fun<T> sendAsync(request: Request<T>): CompletableFuture<T>
}