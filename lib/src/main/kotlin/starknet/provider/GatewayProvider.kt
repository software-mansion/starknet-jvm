package starknet.provider

import kotlinx.serialization.json.*
import starknet.data.types.*

class GatewayProvider(
    private val feederGatewayUrl: String,
    private val gatewayUrl: String,
    override val chainId: StarknetChainId
): Provider  {
    private fun mapCalldataToDecimal(calldata: Calldata): JsonElement {
        return JsonArray(calldata.map {
            JsonPrimitive(it.decString())
        })
    }

    override fun callContract(payload: CallContractPayload): Request<CallContractResponse> {
        var url = "$feederGatewayUrl/call_contract"
        url += "?blockHash=${payload.blockHashOrTag.string()}"

        val decimalCalldata = mapCalldataToDecimal(payload.request.calldata)

        val jsonPayload = Json.encodeToJsonElement(mapOf(
            "contract_address" to payload.request.contractAddress,
            "entry_point_selector" to payload.request.entrypoint,
            "calldata" to decimalCalldata,
        ))

        return Request(url, "POST", emptyList(), jsonPayload.toString(), CallContractResponse.serializer())
    }

    override fun getStorageAt(payload: GetStorageAtPayload): Request<GetStorageAtResponse> {
        var url = "$feederGatewayUrl/get_storage_at"
        url += "?contractAddress=${payload.contractAddress.hexString()}"
        url += "?key=${payload.key.hexString()}"
        url += "?blockHash=${payload.blockHashOrTag.string()}" // TODO: Has to be verified

        return Request(url, "GET", emptyList(), "", GetStorageAtResponse.serializer())
    }

    override fun invokeFunction(payload: InvokeFunctionPayload): Request<InvokeFunctionResponse> {
        val url = "$gatewayUrl/add_transaction"

        val decimalCalldata = mapCalldataToDecimal(payload.invocation.calldata)

        val jsonPayload = Json.encodeToJsonElement(mapOf(
            "type" to JsonPrimitive("INVOKE_FUNCTION"),
            "contract_address" to payload.invocation.contractAddress,
            "entry_point_selector" to payload.invocation.entrypoint,
            "calldata" to decimalCalldata,
            "max_fee" to payload.maxFee,
            "signature" to payload.signature
        ))

        return Request(url, "POST", emptyList(), jsonPayload.toString(), InvokeFunctionResponse.serializer())
    }
}
