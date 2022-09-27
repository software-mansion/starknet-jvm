package com.swmansion.starknet.extensions

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture

internal fun <T, N> Request<T>.compose(mapping: (value: T) -> Request<N>): Request<N> {
    return DependentRequest(this, mapping)
}

internal fun <T, N> Request<T>.map(mapping: (value: T) -> N): Request<N> {
    return MappedRequest(this, mapping)
}

internal class DependentRequest<T, D>(
    private val dependsOn: Request<D>,
    private val mapper: (D) -> Request<T>,
) : Request<T> {

    override fun send(): T {
        return mapper(dependsOn.send()).send()
    }

    override fun sendAsync(): CompletableFuture<T> {
        return dependsOn.sendAsync()
            .thenApplyAsync(mapper)
            .thenComposeAsync(Request<T>::sendAsync)
    }
}

internal class MappedRequest<T, D>(
    private val originalRequest: Request<D>,
    private val mapper: (D) -> T,
) : Request<T> {
    override fun send(): T {
        return mapper(originalRequest.send())
    }

    override fun sendAsync(): CompletableFuture<T> {
        return originalRequest.sendAsync()
            .thenApplyAsync(mapper)
    }
}
