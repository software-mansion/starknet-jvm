package starknet.provider.service

import starknet.data.types.Response
import starknet.provider.Request
import java.util.concurrent.CompletableFuture

interface Service {
    fun<T: Response> send(request: Request<T>): T

    fun<T: Response> sendAsync(request: Request<T>): CompletableFuture<T>

    abstract class ServiceException(message: String) : Exception(message)
}