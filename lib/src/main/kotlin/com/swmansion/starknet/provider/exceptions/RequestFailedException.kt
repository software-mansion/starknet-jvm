package com.swmansion.starknet.provider.exceptions

/**
 * Base exception for all provider errors.
 *
 * @param message error message
 * @param payload payload returned by the service used to communicate with StarkNet
 */
open class RequestFailedException(message: String = "Request failed", val payload: String) : RuntimeException(message) {
    override fun toString(): String {
        return "$message: $payload"
    }
}
