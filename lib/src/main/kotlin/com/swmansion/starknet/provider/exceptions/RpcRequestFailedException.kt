package com.swmansion.starknet.provider.exceptions

/**
 * Exception thrown by rpc provider on request failure.
 *
 * @param code error code returned by the rpc provider
 * @param message error message returned by the rpc provider
 * @param payload payload returned by the service used to communicate with StarkNet
 */
class RpcRequestFailedException(val code: Int, message: String, payload: String) :
    RequestFailedException(message = message, payload = payload)
