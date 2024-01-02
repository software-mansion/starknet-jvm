package com.swmansion.starknet.provider.exceptions

/**
 * Base exception for all provider errors.
 *
 * @param message error message
 * @param payload payload returned by the service used to communicate with Starknet
 * @param data data returned by the rpc provider
 */
open class RequestFailedException(message: String = "Request failed", val data: String? = null, val payload: String) : RuntimeException(message) {
    override fun toString(): String {
        return when (data) {
            null -> "$message: $payload"
            else -> "$message: $data : $payload"
        }
    }
}
