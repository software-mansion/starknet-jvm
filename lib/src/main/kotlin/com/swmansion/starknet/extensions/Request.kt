package com.swmansion.starknet.extensions

import com.swmansion.starknet.provider.Request
import java.util.concurrent.CompletableFuture

internal fun <T, N> Request<T>.compose(mapping: (value: T) -> Request<N>): Request<N> {
    return DependentRequest(this, mapping)
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
