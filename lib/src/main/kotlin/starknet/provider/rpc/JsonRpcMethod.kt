package starknet.provider.rpc

/**
 * Enum with JSON-RPC methods.
 *
 * @param methodName method name to be converted to enum value
 */
enum class JsonRpcMethod(val methodName: String) {
    CALL("starknet_call"),
    INVOKE_TRANSACTION("starknet_addInvokeTransaction"),
    GET_STORAGE_AT("starknet_getStorageAt")
}
