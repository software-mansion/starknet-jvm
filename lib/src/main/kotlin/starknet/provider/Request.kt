package starknet.provider

import java.util.concurrent.CompletableFuture

/**
 * An interface implemented by all return values of providers.
 */
interface Request<T> {
    fun send(): T

    fun sendAsync(): CompletableFuture<T>
}
