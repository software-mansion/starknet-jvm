package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.TransactionStatus
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.ContractDeployer
import starknet.utils.DevnetClient
import java.nio.file.Path

class StandardAccountTest {
    companion object {
        @JvmStatic
        private val devnetClient = DevnetClient(port = 5051)
        private val signer = StarkCurveSigner(Felt(1234))

        private lateinit var gatewayProvider: GatewayProvider
        private lateinit var rpcProvider: JsonRpcProvider
        private lateinit var balanceContractAddress: Felt
        private lateinit var accountAddress: Felt

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

            deployAccount()
        }

        private fun deployAccount() {
            val contractDeployer = ContractDeployer.deployInstance(devnetClient)
            val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/account.json"))
            accountAddress = contractDeployer.deployContract(classHash, calldata = listOf(signer.publicKey))
            devnetClient.prefundAccount(accountAddress)
        }

        data class AccountAndProvider(val account: Account, val provider: Provider)

        @JvmStatic
        fun getAccounts(): List<AccountAndProvider> {
            return listOf(
                AccountAndProvider(
                    StandardAccount(
                        gatewayProvider,
                        accountAddress,
                        signer,
                    ),
                    gatewayProvider,
                ),
            )
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.close()
        }
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `constructor signer creation`(accountAndProvider: AccountAndProvider) {
        val (_, provider) = accountAndProvider
        val privateKey = Felt(1234)
        val account = StandardAccount(provider, Felt.ZERO, privateKey)

        assertNotNull(account.signer)
        assertEquals(privateKey, account.signer.privateKey)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `get nonce test`(accountAndProvider: AccountAndProvider) {
        val (account, _) = accountAndProvider
        val nonce = account.getNonce()

        assertNotNull(nonce)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate fee`(accountAndProvider: AccountAndProvider) {
        val (account, _) = accountAndProvider
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
    fun `sign single call test`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val params = ExecutionParams(
            maxFee = Felt(1000000000000000),
            nonce = account.getNonce().send(),
        )

        val payload = account.sign(call, params)
        val request = provider.invokeFunction(payload)
        val response = request.send()
        val receipt = provider.getTransactionReceipt(response.transactionHash).send()

        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute single call`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val result = account.execute(call).send()
        assertNotNull(result)

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute single call with specific fee`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val maxFee = Felt(10000000L)
        val result = account.execute(call, maxFee).send()
        assertNotNull(result)

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.actualFee < maxFee)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign multiple calls test`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider

        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val params = ExecutionParams(
            maxFee = Felt(1000000000000000),
            nonce = account.getNonce().send(),
        )

        val payload = account.sign(listOf(call, call, call), params)
        val response = provider.invokeFunction(payload).send()
        val receipt = provider.getTransactionReceipt(response.transactionHash).send()

        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute multiple calls`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
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

        val result = account.execute(listOf(call1, call2)).send()
        assertNotNull(result)

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `two executes with single call`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val result = account.execute(call).send()
        assertNotNull(result)

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()
        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)

        val call2 = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(20)),
            entrypoint = "increase_balance",
        )

        val result2 = account.execute(call2).send()
        assertNotNull(result)

        val receipt2 = provider.getTransactionReceipt(result2.transactionHash).send()
        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt2.status)
    }
}
