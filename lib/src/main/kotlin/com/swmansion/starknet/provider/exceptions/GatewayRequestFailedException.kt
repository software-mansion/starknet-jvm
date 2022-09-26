package com.swmansion.starknet.provider.exceptions

/**
 * Exception thrown by gateway provider on request failure.
 *
 * @param code error code returned by the gateway
 * @param message error message returned by the gateway
 * @param payload payload returned by the service used to communicate with StarkNet
 */
class GatewayRequestFailedException(val code: Int, message: String, payload: String) :
    RequestFailedException(message = message, payload = payload)
