package com.swmansion.starknet.provider.exceptions

open class RequestFailedException(message: String = "Request failed", val payload: String) : Exception(message)
