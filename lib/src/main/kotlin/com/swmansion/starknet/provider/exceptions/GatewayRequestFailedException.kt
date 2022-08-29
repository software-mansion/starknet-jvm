package com.swmansion.starknet.provider.exceptions

class GatewayRequestFailedException(val code: Int, message: String, payload: String) :
    RequestFailedException(message = message, payload = payload)
