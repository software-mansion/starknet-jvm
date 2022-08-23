package com.swmansion.starknet.provider.exceptions

class RpcRequestFailedException(val code: Int, message: String) :
    RequestFailedException(message)
