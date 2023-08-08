package starknet.utils

import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.*
import kotlinx.serialization.json.*
import org.mockito.kotlin.*

object MockUtils {
    @JvmStatic
    fun mockUpdatedReceiptRpcProvider(sourceProvider: JsonRpcProvider): JsonRpcProvider {
        val mockHttpService = spy(OkHttpService())

        doAnswer { invocation ->
            val originalHttpResponse = invocation.callRealMethod() as HttpResponse
            val originalJson = Json.parseToJsonElement(originalHttpResponse.body) as JsonObject
            val status = originalJson["result"]!!.jsonObject["status"]?.jsonPrimitive?.content
                ?: TransactionFinalityStatus.ACCEPTED_ON_L1.toString()

            val acceptedStatuses = listOf(
                TransactionStatus.ACCEPTED_ON_L1.toString(),
                TransactionStatus.ACCEPTED_ON_L2.toString(),
            )
            val executionStatus = when (status) {
                in acceptedStatuses -> TransactionExecutionStatus.SUCCEEDED.toString()
                else -> TransactionExecutionStatus.REVERTED.toString()
            }

            val modifiedJson = JsonObject(
                originalJson["result"]!!.jsonObject.toMutableMap().apply {
                    remove("status")
                    put("finality_status", JsonPrimitive(status))
                    put("execution_status", JsonPrimitive(executionStatus))
                },
            )
            val mergedJson = JsonObject(
                originalJson.toMutableMap().apply {
                    this["result"] = modifiedJson
                },
            )
            return@doAnswer HttpResponse(
                isSuccessful = originalHttpResponse.isSuccessful,
                code = originalHttpResponse.code,
                body = mergedJson.toString(),
            )
        }.`when`(mockHttpService).send(any())

        return JsonRpcProvider(
            sourceProvider.url,
            sourceProvider.chainId,
            mockHttpService,
        )
    }
}
