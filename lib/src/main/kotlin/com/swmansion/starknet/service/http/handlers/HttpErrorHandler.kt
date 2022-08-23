package com.swmansion.starknet.service.http.handlers

internal interface HttpErrorHandler {
    fun handle(response: String): Nothing
}
