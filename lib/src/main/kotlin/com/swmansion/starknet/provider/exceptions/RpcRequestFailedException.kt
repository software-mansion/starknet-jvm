package com.swmansion.starknet.provider.exceptions

/**
 * Exception thrown by rpc provider on request failure.
 *
 * @param code error code returned by the rpc provider
 * @param message error message returned by the rpc provider
 * @param payload payload returned by the service used to communicate with Starknet
 * @param data data returned by the rpc provider
 */
class RpcRequestFailedException(val code: Int, message: String, data: String? = null, payload: String) :
    RequestFailedException(message = message, data = data, payload = payload)
