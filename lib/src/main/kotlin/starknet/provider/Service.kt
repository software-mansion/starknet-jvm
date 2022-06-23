package starknet.provider

import starknet.data.types.Response
import java.util.concurrent.CompletableFuture

interface Service {
    fun<T: Response> send(request: Request<T>): T

    fun<T: Response> sendAsync(request: Request<T>): CompletableFuture<T>
}