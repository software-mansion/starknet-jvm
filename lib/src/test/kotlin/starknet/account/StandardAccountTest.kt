package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
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
import starknet.utils.MockUtils
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

                val (deployAddress, _) = devnetClient.deployContract(Path.of("src/test/resources/compiled_v0/providerTest.json"))
                balanceContractAddress = deployAddress

                deployAccount()
            } catch (ex: Exception) {
                devnetClient.close()
                throw ex
            }
        }

        private fun deployAccount() {
            val contractDeployer = ContractDeployer.deployInstance(devnetClient)
            val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled_v0/account.json"))
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
    fun `estimate fee for declare v1 transaction`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x320aba87b66c023b2db943b9d32bc0f8e3d72625b475e1dc77e4d2f21721d43")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000)),
        )

        val signedTransaction = TransactionFactory.makeDeclareV1Transaction(
            classHash = classHash,
            senderAddress = declareTransactionPayload.senderAddress,
            contractDefinition = declareTransactionPayload.contractDefinition,
            chainId = provider.chainId,
            nonce = nonce,
            maxFee = declareTransactionPayload.maxFee,
            signature = declareTransactionPayload.signature,
            version = declareTransactionPayload.version,
        )

        val feeEstimate = provider.getEstimateFee(listOf(signedTransaction.toPayload()), BlockTag.LATEST)

        assertNotNull(feeEstimate)
    }

    // @Test
    // fun `mock estimate message fee`() {
    //     // Note for future developers experiencing failures in this test:
    //     // This test is designed with RPC 0.3.1 in mind (which was never released).
    //     // The schema will be changed as of RPC 0.4.0 and provider.getEstimateMessageFee will have a different payload.

    //     val gasConsumed = Felt(45100)
    //     val gasPrice = Felt(2)
    //     val overallFee = Felt(45100 * 2)
    //     val mockedResponse =
    //         """
    //     {
    //         "id": 0,
    //         "jsonrpc": "2.0",
    //         "result":
    //         {
    //             "gas_consumed": "${gasConsumed.hexString()}",
    //             "gas_price": "${gasPrice.hexString()}",
    //             "overall_fee": "${overallFee.hexString()}"
    //         }
    //     }
    //         """.trimIndent()
    //     val blockNumber = 123456789
    //     val httpService = mock<HttpService> {
    //         on { send(any()) } doReturn HttpResponse(
    //             isSuccessful = true,
    //             code = 200,
    //             body = mockedResponse,
    //         )
    //     }
    //     val messageCall = Call(
    //         contractAddress = balanceContractAddress,
    //         calldata = listOf(Felt(10)),
    //         entrypoint = "increase_balance",
    //     )

    //     val provider = JsonRpcProvider(devnetClient.rpcUrl, StarknetChainId.TESTNET, httpService)
    //     val request = provider.getEstimateMessageFee(
    //         message = messageCall,
    //         senderAddress = balanceContractAddress,
    //         blockNumber = blockNumber,
    //     )
    //     val response = request.send()

    //     assertNotNull(response)
    //     assertEquals(gasPrice, response.gasPrice)
    //     assertEquals(gasConsumed, response.gasConsumed)
    //     assertEquals(overallFee, response.overallFee)
    // }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v1 transaction`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x37475b8cd1e7360416bae6bc332ba4bc50936cd2d0f9f2207507acbac172e8d")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )

        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()

        assertNotNull(result)
        assertNotNull(receipt)
        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v2 transaction`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

        val contractCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.json").readText()
        val casmCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.casm").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            contractCasmDefinition,
            ExecutionParams(nonce, Felt(1000000000000000)),
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()
        assertNotNull(result)
        assertNotNull(receipt)
        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign single call test`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

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

        val receipt = receiptProvider.getTransactionReceipt(response.transactionHash).send()
        assertTrue(receipt.isAccepted)
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
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val result = account.execute(call).send()
        assertNotNull(result)

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute single call with specific fee`(accountAndProvider: AccountAndProvider) {
        // Note to future developers experiencing failures in this test:
        // If the max fee is too low the transaction may fail with 500 error code (RPC).

        val (account, provider) = accountAndProvider
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val maxFee = Felt(1000000000000000L)
        val result = account.execute(call, maxFee).send()
        assertNotNull(result)

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.actualFee!! < maxFee)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign multiple calls test`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

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
        val receipt = receiptProvider.getTransactionReceipt(response.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute multiple calls`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

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

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `two executes with single call`(accountAndProvider: AccountAndProvider) {
        val (account, sourceProvider) = accountAndProvider
        val receiptProvider = when (sourceProvider) {
            is GatewayProvider -> sourceProvider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(sourceProvider)
            else -> throw IllegalStateException("Unknown provider type")
        }

        val call = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )

        val result = account.execute(call).send()
        assertNotNull(result)

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()
        assertTrue(receipt.isAccepted)

        val call2 = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(20)),
            entrypoint = "increase_balance",
        )

        val result2 = account.execute(call2).send()
        assertNotNull(result)

        val receipt2 = receiptProvider.getTransactionReceipt(result2.transactionHash).send()
        assertTrue(receipt2.isAccepted)
    }

    @Test
    fun `cairo1 account calldata`() {
        val call1 = Call(
            contractAddress = balanceContractAddress,
            calldata = listOf(Felt(10), Felt(20), Felt(30)),
            entrypoint = "increase_balance",
        )

        val call2 = Call(
            contractAddress = Felt(999),
            calldata = listOf(),
            entrypoint = "empty_calldata",
        )

        val call3 = Call(
            contractAddress = Felt(123),
            calldata = listOf(Felt(100), Felt(200)),
            entrypoint = "another_method",
        )

        val account = StandardAccount(accountAddress, signer, rpcProvider, Felt(1))
        val params = ExecutionParams(Felt.ZERO, Felt.ZERO)
        val signedTx = account.sign(listOf(call1, call2, call3), params)

        val expectedCalldata = listOf(
            Felt(3),
            balanceContractAddress,
            selectorFromName("increase_balance"),
            Felt(3),
            Felt(10),
            Felt(20),
            Felt(30),
            Felt(999),
            selectorFromName("empty_calldata"),
            Felt(0),
            Felt(123),
            selectorFromName("another_method"),
            Felt(2),
            Felt(100),
            Felt(200),
        )

        assertEquals(expectedCalldata, signedTx.calldata)

        val signedEmptyTx = account.sign(listOf<Call>(), params)

        assertEquals(listOf(Felt.ZERO), signedEmptyTx.calldata)
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
            nonce = Felt.ZERO,
            forFeeEstimate = true,
        )
        assertEquals(payloadForFeeEstimation.version, Felt(BigInteger("340282366920938463463374607431768211457")))

        val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
        assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `deploy account`(provider: Provider) {
        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

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

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()
        assertTrue(receipt.isAccepted)
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
            nonce = Felt.ONE,
            forFeeEstimate = true,
        )
        assertEquals(payloadForFeeEstimation.version, Felt(BigInteger("340282366920938463463374607431768211457")))

        assertThrows(RequestFailedException::class.java) {
            provider.deployAccount(payloadForFeeEstimation).send()
        }
    }

    @Test
    fun `simulate transactions`() {
        val account = StandardAccount(accountAddress, signer, rpcProvider)

        val nonce = account.getNonce().send()
        val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(1000)))
        val params = ExecutionParams(nonce, Felt(1000000000))

        val invokeTx = account.sign(call, params)

        val privateKey = Felt(22222)
        val publicKey = StarknetCurve.getPublicKey(privateKey)
        val accountClassHash = accountClassHash
        val salt = Felt.ONE
        val calldata = listOf(publicKey)

        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountClassHash,
            calldata = calldata,
            salt = salt,
        )
        val newAccount = StandardAccount(
            address,
            privateKey,
            rpcProvider,
        )
        devnetClient.prefundAccount(address)
        val deployAccountTx = newAccount.signDeployAccount(
            classHash = accountClassHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.fromHex("0x11fcc58c7f7000"),
        )

        val simulationResult = rpcProvider.simulateTransactions(listOf(invokeTx, deployAccountTx), BlockTag.LATEST, setOf()).send()
        assertEquals(2, simulationResult.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)

        val invokeTxWithoutSignature = InvokeTransactionPayload(invokeTx.senderAddress, invokeTx.calldata, emptyList(), invokeTx.maxFee, invokeTx.version, invokeTx.nonce)
        val deployAccountTxWithoutSignature = DeployAccountTransactionPayload(deployAccountTx.classHash, deployAccountTx.salt, deployAccountTx.constructorCalldata, deployAccountTx.version, deployAccountTx.nonce, deployAccountTx.maxFee, emptyList())

        val simulationResult2 = rpcProvider.simulateTransactions(listOf(invokeTxWithoutSignature, deployAccountTxWithoutSignature), BlockTag.LATEST, setOf(SimulationFlag.SKIP_VALIDATE)).send()

        assertEquals(2, simulationResult2.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)
    }
}
