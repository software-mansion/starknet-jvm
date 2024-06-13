package com.swmansion.starknet.data.types

data class RequestResult<out T>(val result: Result<T>) {
    companion object {
        fun <T> success(value: T) = RequestResult(Result.success(value))

        fun <T> failure(throwable: Throwable) = RequestResult(Result.failure(throwable))
    }

    val isSuccess: Boolean
        get() = result.isSuccess

    val isFailure: Boolean
        get() = result.isFailure

    fun getOrNull(): T? = result.getOrNull()

    fun exceptionOrNull(): Throwable? = result.exceptionOrNull()

    fun getOrThrow(): T = result.getOrThrow()
}
