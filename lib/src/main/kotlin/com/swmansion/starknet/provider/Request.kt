package com.swmansion.starknet.provider

import java.util.concurrent.CompletableFuture

/**
 * An interface implemented by all return values of providers.
 */
interface Request<T> {
    /**
     * Send a request synchronously
     *
     * @return a result of the request
     */
    fun send(): T

    /**
     * Send a request asynchronously
     *
     * @return CompletableFuture with the request result
     */
    fun sendAsync(): CompletableFuture<T>
}
