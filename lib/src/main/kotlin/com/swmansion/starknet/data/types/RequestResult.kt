package com.swmansion.starknet.data.types

class RequestResult<out T>(val result: Result<T>) {

    companion object {
        fun <T> success(value: T): RequestResult<T> = RequestResult(Result.success(value))

        fun <T> failure(throwable: Throwable): RequestResult<T> =
            RequestResult(Result.failure(throwable))
    }

    val isSuccess: Boolean
        get() = result.isSuccess

    val isFailure: Boolean
        get() = result.isFailure

    fun getOrNull(): T? = result.getOrNull()

    fun exceptionOrNull(): Throwable? = result.exceptionOrNull()

    fun getOrThrow(): T = result.getOrThrow()
}
