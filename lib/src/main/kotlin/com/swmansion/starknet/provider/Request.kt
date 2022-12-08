package com.swmansion.starknet.provider

import com.swmansion.starknet.provider.exceptions.RequestFailedException
import java.util.concurrent.CompletableFuture
import kotlin.jvm.Throws

/**
 * An interface implemented by all return values of providers.
 */
interface Request<T> {
    /**
     * Send a request synchronously
     *
     * @return a result of the request
     */
    @Throws(RequestFailedException::class)
    fun send(): T

    /**
     * Send a request asynchronously
     *
     * @return CompletableFuture with the request result
     */
    fun sendAsync(): CompletableFuture<T>
}
