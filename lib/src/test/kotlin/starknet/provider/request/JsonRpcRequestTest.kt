package starknet.provider.request

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import starknet.utils.DevnetClient
import java.nio.file.Paths

class JsonRpcRequestTest {

    companion object {
        private val devnetClient = DevnetClient(
            port = 5052,
            accountDirectory = Paths.get("src/test/resources/accounts/provider_test"),
            contractsDirectory = Paths.get("src/test/resources/contracts"),
        )
        val rpcUrl = devnetClient.rpcUrl
        private val provider = JsonRpcProvider(rpcUrl)

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()
            } catch (ex: Exception) {
                devnetClient.close()
                throw ex
            }
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.close()
        }
    }

    @Test
    fun `rpc provider sends multiple batch call request`() {
        val ethContractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")

        val call = Call(
            ethContractAddress,
            Felt.fromHex("0x2e4263afad30923c891518314c3c95dbe830a16874e8abc5777a9a20b54c76e"),
            listOf(Felt.fromHex("0x07f6331182b9bcbf9c1a5943e309c05399a935d170f7f07494cdf7a174cd7527")),
        )
        val calls = listOf(provider.callContract(call), provider.callContract(call))
        val request = provider.sendBatchRequest(calls)
        var response = emptyList<List<Felt>>()

        assertAll({ response = request.send() })
        assertEquals(response.size, calls.size)
    }
}
