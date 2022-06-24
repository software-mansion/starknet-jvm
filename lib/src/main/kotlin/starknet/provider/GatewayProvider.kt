package starknet.provider

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import starknet.data.types.*
import starknet.provider.service.HttpService

class GatewayProvider(
    val feederGatewayUrl: String,
    val gatewayUrl: String,
    override val chainId: StarknetChainId
): Provider  {
    override fun callContract(invokeTransaction: Invocation): Request<CallContractResponse> {
        val service = HttpService(feederGatewayUrl + "call_contract", "get")

        val payload = Json.encodeToString(invokeTransaction)

        return Request(service, payload, CallContractResponse.serializer())
    }
}