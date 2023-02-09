package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.DeployAccountTransaction
import com.swmansion.starknet.data.types.transactions.TransactionFactory
import com.swmansion.starknet.data.types.transactions.TransactionStatus
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import starknet.data.loadTypedData
import starknet.utils.ContractDeployer
import starknet.utils.DevnetClient
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class StandardAccountTest {
    companion object {
        @JvmStatic
        private val devnetClient =
            DevnetClient(port = 5051, accountDirectory = Paths.get("src/test/resources/standard_account_test_account"))
        private val signer = StarkCurveSigner(Felt(1234))

        private lateinit var gatewayProvider: GatewayProvider
        private lateinit var rpcProvider: JsonRpcProvider
        private lateinit var balanceContractAddress: Felt
        private lateinit var accountAddress: Felt
        private lateinit var accountClassHash: Felt

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
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
            } catch (ex: Exception) {
                devnetClient.close()
                throw ex
            }
        }

        private fun deployAccount() {
            val contractDeployer = ContractDeployer.deployInstance(devnetClient)
            val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled/account.json"))
            accountClassHash = classHash
            accountAddress = contractDeployer.deployContract(classHash, calldata = listOf(signer.publicKey))
            devnetClient.prefundAccount(accountAddress)
        }

        data class AccountAndProvider(val account: Account, val provider: Provider)

        @JvmStatic
        private fun getProviders(): List<Provider> = listOf(gatewayProvider, rpcProvider)

        @JvmStatic
        fun getAccounts(): List<AccountAndProvider> {
            return listOf(
                AccountAndProvider(
                    StandardAccount(
                        accountAddress,
                        signer,
                        gatewayProvider,
                    ),
                    gatewayProvider,
                ),
                AccountAndProvider(
                    StandardAccount(
                        accountAddress,
                        signer,
                        rpcProvider,
                    ),
                    rpcProvider,
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
    fun `creating account with private key`(accountAndProvider: AccountAndProvider) {
        val (_, provider) = accountAndProvider
        val privateKey = Felt(1234)
        StandardAccount(Felt.ZERO, privateKey, provider)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `get nonce test`(accountAndProvider: AccountAndProvider) {
        val (account, _) = accountAndProvider
        val nonce = account.getNonce().send()
        assert(nonce >= Felt.ZERO)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `get nonce twice`(accountAndProvider: AccountAndProvider) {
        val (account, _) = accountAndProvider
        val startNonce = account.getNonce().send()
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )
        account.execute(call).send()

        val endNonce = account.getNonce().send()

        assertEquals(
            Felt(startNonce.value + Felt.ONE.value),
            endNonce,
        )
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate fee for invoke transaction`(accountAndProvider: AccountAndProvider) {
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
    fun `estimate fee for declare transaction`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val contractCode = Path.of("src/test/resources/compiled/providerTest.json").readText()
        val contractDefinition = ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x399998c787e0a063c3ac1d2abac084dcbe09954e3b156d53a8c43a02aa27d35")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000)),
        )

        val signedTransaction = TransactionFactory.makeDeclareTransaction(
            classHash = classHash,
            senderAddress = declareTransactionPayload.senderAddress,
            contractDefinition = declareTransactionPayload.contractDefinition,
            chainId = provider.chainId,
            nonce = nonce,
            maxFee = declareTransactionPayload.maxFee,
            signature = declareTransactionPayload.signature,
            version = declareTransactionPayload.version,
        )

        val feeEstimate = provider.getEstimateFee(signedTransaction.toPayload(), BlockTag.LATEST)

        assertNotNull(feeEstimate)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare transaction`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val contractCode = Path.of("src/test/resources/compiled/providerTest.json").readText()
        val contractDefinition = ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x399998c787e0a063c3ac1d2abac084dcbe09954e3b156d53a8c43a02aa27d35")

        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000)),
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertNotNull(result)
        assertNotNull(receipt)
        assertTrue(receipt.isAccepted)
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
    fun `sign TypedData`(accountAndProvider: AccountAndProvider) {
        val (account, _) = accountAndProvider
        val typedData = loadTypedData("typed_data_struct_array_example.json")

        // Sign typedData
        val signature = account.signTypedData(typedData)
        assertTrue(signature.isNotEmpty())

        // Verify the signature
        val request = account.verifyTypedDataSignature(typedData, signature)
        val isValid = request.send()
        assertTrue(isValid)

        // Verify invalid signature does not pass
        val request2 = account.verifyTypedDataSignature(typedData, listOf(Felt.ONE, Felt.ONE))
        val isValid2 = request2.send()
        assertFalse(isValid2)
    }

    @Test
    fun `sign TypedData rethrows exceptions other than signature related`() {
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(
                false,
                500,
                """
                {
                    "something": "broke"    
                }
                """.trimIndent(),
            )
        }
        val provider = GatewayProvider.makeTestnetProvider(httpService)
        val account = StandardAccount(Felt.ONE, Felt.ONE, provider)

        val typedData = loadTypedData("typed_data_struct_array_example.json")
        val signature = account.signTypedData(typedData)
        assertTrue(signature.isNotEmpty())

        val request = account.verifyTypedDataSignature(typedData, signature)
        assertThrows(RequestFailedException::class.java) {
            request.send()
        }
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

        assertTrue(receipt.actualFee!! < maxFee)
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

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `estimate deploy account fee`(provider: Provider) {
        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = accountClassHash
        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            forFeeEstimate = true,
        )
        assertEquals(payloadForFeeEstimation.version, Felt(BigInteger("340282366920938463463374607431768211457")))

        val feePayload = provider.getEstimateFee(payloadForFeeEstimation).send()
        assertTrue(feePayload.overallFee.value > Felt.ONE.value)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `deploy account`(provider: Provider) {
        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = accountClassHash
        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
        )
        devnetClient.prefundAccount(address)

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payload = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            // 10*fee from estimate deploy account fee
            maxFee = Felt.fromHex("0x11fcc58c7f7000"),
        )

        val response = provider.deployAccount(payload).send()

        // Make sure the address matches the calculated one
        assertEquals(address, response.address)

        // Make sure tx matches what we sent
        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransaction
        assertEquals(payload.classHash, tx.classHash)
        assertEquals(payload.salt, tx.contractAddressSalt)
        assertEquals(payload.constructorCalldata, tx.constructorCalldata)
        assertEquals(payload.version, tx.version)
        assertEquals(payload.nonce, tx.nonce)
        assertEquals(payload.maxFee, tx.maxFee)
        assertEquals(payload.signature, tx.signature)

        // Invoke function to make sure the account was deployed properly
        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )
        val result = account.execute(call).send()
        val receipt = provider.getTransactionReceipt(result.transactionHash).send()
        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `send transaction signed for fee estimation`(provider: Provider) {
        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = accountClassHash
        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            forFeeEstimate = true,
        )
        assertEquals(payloadForFeeEstimation.version, Felt(BigInteger("340282366920938463463374607431768211457")))

        assertThrows(RequestFailedException::class.java) {
            provider.deployAccount(payloadForFeeEstimation).send()
        }
    }
}
