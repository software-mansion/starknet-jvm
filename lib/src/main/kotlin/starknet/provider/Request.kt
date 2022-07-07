package starknet.provider

import java.util.concurrent.CompletableFuture

interface Request<T> {
    fun send(): T

    fun sendAsync(): CompletableFuture<T>
}