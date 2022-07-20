package starknet.provider.rpc

/**
 * Enum with JSON-RPC methods.
 *
 * @param methodName method name to be converted to enum value
 */
enum class JsonRpcMethod(val methodName: String) {
    CALL("starknet_call"),
    INVOKE_TRANSACTION("starknet_addInvokeTransaction"),
    GET_STORAGE_AT("starknet_getStorageAt"),
    GET_CLASS("starknet_getClass"),
    GET_CLASS_AT("starknet_getClassAt"),
    GET_CLASS_HASH_AT("starknet_getClassHashAt")
}
