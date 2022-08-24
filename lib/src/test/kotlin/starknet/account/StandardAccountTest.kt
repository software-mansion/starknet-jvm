package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.DevnetClient
import java.nio.file.Path

class StandardAccountTest {
    companion object {
        @JvmStatic
        private val devnetClient = DevnetClient()
        private lateinit var gatewayProvider: GatewayProvider
        private lateinit var rpcProvider: JsonRpcProvider
        private lateinit var balanceContractAddress: Felt

        @JvmStatic
        @BeforeAll
        fun before() {
            devnetClient.start()

            gatewayProvider = GatewayProvider(
                devnetClient.feederGatewayUrl,
                devnetClient.gatewayUrl,
                StarknetChainId.TESTNET,
            )

            rpcProvider = JsonRpcProvider(
                devnetClient.rpcUrl,
                StarknetChainId.TESTNET,
            )

            val (deployAddress, _) = devnetClient.deployContract(Path.of("src/test/resources/compiled/providerTest.json"))
            balanceContractAddress = deployAddress
        }

        @JvmStatic
        fun getAccounts(): List<Account> {
            val account1 = StandardAccount(
                gatewayProvider,
                Felt.fromHex("0x5fa2c31b541653fc9db108f7d6857a1c2feda8e2abffbfa4ab4eaf1fcbfabd8"),
                Felt.fromHex("0x5421eb02ce8a5a972addcd89daefd93c"),
            )

            val account2 = StandardAccount(
                gatewayProvider,
                Felt.fromHex("0x7598217a5d6159c7dc954996eeafacf96b782524a97c44e417e10a8353afbd4"),
                Felt.fromHex("0xea119d5bfc687eafb3a40275fae4a74e"),
            )

//            val account3 = StandardAccount(
//                rpcProvider,
//                Felt.fromHex("0x2000c94da25e3772c290db227f1f57358c65d3bdda517dcd3dcbdbb04141900"),
//                Felt.fromHex("0xde49194669e58e796a5e2915289ae880"),
//            )

            return listOf(account1, account2)
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.close()
        }
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `get nonce test`(account: Account) {
        val nonce = account.getNonce()

        assertEquals(nonce, Felt.ZERO)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate fee`(account: Account) {
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val feeEstimate = account.estimateFee(call)

        assertNotNull(feeEstimate)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute single call`(account: Account) {
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val result = account.execute(call)
        assertNotNull(result)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute multiple calls`(account: Account) {
        val call1 = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val call2 = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val result = account.execute(listOf(call1, call2))
        assertNotNull(result)
    }
}
