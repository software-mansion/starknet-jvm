package com.swmansion.starknet.data.types

sealed class StarknetResult<out T> {

    data class Success<T>(val value: T) : StarknetResult<T>()

    data class Failure<T>(val exception: Exception) : StarknetResult<T>()

    val isSuccessful: Boolean get() = this is Success<T>
    val isFailure: Boolean get() = this is Failure<T>

    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            else -> null
        }

    fun exceptionOrNull(): Exception? =
        when (this) {
            is Failure -> exception
            else -> null
        }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw exception
    }
}
