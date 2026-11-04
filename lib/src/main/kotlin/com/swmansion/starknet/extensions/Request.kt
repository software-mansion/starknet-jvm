package com.swmansion.starknet.extensions

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture

internal fun <T, N> Request<T>.compose(mapping: (value: T) -> Request<N>): Request<N> = DependentRequest(this, mapping)

internal fun <T, N> Request<T>.map(mapping: (value: T) -> N): Request<N> = MappedRequest(this, mapping)

internal class DependentRequest<T, D>(
    private val dependsOn: Request<D>,
    private val mapper: (D) -> Request<T>,
) : Request<T> {
    override fun send(): T = mapper(dependsOn.send()).send()

    override fun sendAsync(): CompletableFuture<T> =
        dependsOn
            .sendAsync()
            .thenApplyAsync(mapper)
            .thenComposeAsync(Request<T>::sendAsync)
}

internal class MappedRequest<T, D>(
    private val originalRequest: Request<D>,
    private val mapper: (D) -> T,
) : Request<T> {
    override fun send(): T = mapper(originalRequest.send())

    override fun sendAsync(): CompletableFuture<T> =
        originalRequest
            .sendAsync()
            .thenApplyAsync(mapper)
}
