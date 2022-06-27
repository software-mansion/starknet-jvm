package starknet.provider

import kotlinx.serialization.json.*
import starknet.data.types.*

class GatewayProvider(
    val feederGatewayUrl: String,
    val gatewayUrl: String,
    override val chainId: StarknetChainId
): Provider  {
    override fun callContract(call: Call, callParams: CallExtraParams): Request<CallContractResponse> {
        var url = gatewayUrl + "call_contract"

        url += "?block_hash=${callParams.blochHashOrTag.string()}"

        // Gateway requires calldata values to be in decimal form
        val decimalCalldata = JsonArray(call.calldata.map {
            JsonPrimitive(it.decString())
        })

        val payload = Json.encodeToJsonElement(call)

        val fixedPayload = payload.jsonObject.toMutableMap()
        fixedPayload.set("calldata", decimalCalldata)

        val payloadString = JsonObject(fixedPayload).toString()

        return Request(url, "POST", emptyList(), payloadString, CallContractResponse.serializer())
    }
}
