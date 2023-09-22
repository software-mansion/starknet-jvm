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
import com.swmansion.starknet.signer.Signer
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
import starknet.utils.DevnetClient
import starknet.utils.LegacyContractDeployer
import starknet.utils.LegacyDevnetClient
import starknet.utils.MockUtils
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class StandardAccountTest {
    companion object {
        @JvmStatic
        private val legacyDevnetClient = LegacyDevnetClient(
            port = 5051,
            accountDirectory = Paths.get("src/test/resources/accounts_legacy/standard_account_test"),
        )

        private val devnetClient = DevnetClient(
            port = 5061,
            accountDirectory = Paths.get("src/test/resources/accounts/standard_account_test"),
            contractsDirectory = Paths.get("src/test/resources/contracts"),
        )

        private lateinit var legacySigner: Signer
        private lateinit var signer: Signer

        private val rpcProvider = JsonRpcProvider(
            devnetClient.rpcUrl,
            StarknetChainId.TESTNET,
        )
        private val legacyRpcProvider = JsonRpcProvider(
            legacyDevnetClient.rpcUrl,
            StarknetChainId.TESTNET,
        )
        private val legacyGatewayProvider = GatewayProvider(
            legacyDevnetClient.feederGatewayUrl,
            legacyDevnetClient.gatewayUrl,
            StarknetChainId.TESTNET,
        )

        private lateinit var legacyDevnetAddressBook: AddressBook
        private lateinit var devnetAddressBook: AddressBook

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()
                legacyDevnetClient.start()

                // Prepare devnet address book
                val accountDetails = devnetClient.createDeployAccount("standard_account_test").details
                signer = StarkCurveSigner(accountDetails.privateKey)
                val balanceContractAddress = devnetClient.declareDeployContract("Balance").contractAddress
                devnetAddressBook = AddressBook(
                    accountContractClassHash = DevnetClient.accountContractClassHash,
                    accountAddress = accountDetails.address,
                    balanceContractAddress = balanceContractAddress,
                )

                // Prepare legacy devnet address book
                legacySigner = StarkCurveSigner(privateKey = Felt(1234))
                val legacyBalanceContractAddress = legacyDevnetClient.deployContract(Path.of("src/test/resources/compiled_v0/providerTest.json")).address
                val legacyContractDeployer = LegacyContractDeployer.deployInstance(legacyDevnetClient)
                val (legacyAccountClassHash, _) = legacyDevnetClient.declareContract(Path.of("src/test/resources/compiled_v0/account.json"))
                val legacyAccountAddress = legacyContractDeployer.deployContract(legacyAccountClassHash, calldata = listOf(legacySigner.publicKey))
                legacyDevnetClient.prefundAccount(legacyAccountAddress)
                legacyDevnetAddressBook = AddressBook(
                    accountAddress = legacyAccountAddress,
                    accountContractClassHash = legacyAccountClassHash,
                    balanceContractAddress = legacyBalanceContractAddress,
                )
            } catch (ex: Exception) {
                devnetClient.close()
                legacyDevnetClient.close()
                throw ex
            }
        }

        data class AccountParameters(
            val account: Account,
            val provider: Provider,
            val addressBook: AddressBook,
        )

        data class ProviderParameters(
            val provider: Provider,
            val addressBook: AddressBook,
        )

        data class AddressBook(
            val accountContractClassHash: Felt,
            val accountAddress: Felt,
            val balanceContractAddress: Felt,
        )

        @JvmStatic
        fun getProviders(): List<ProviderParameters> {
            return listOf(
                ProviderParameters(
                    legacyGatewayProvider,
                    legacyDevnetAddressBook,
                ),
                ProviderParameters(
                    rpcProvider,
                    devnetAddressBook,
                ),
            )
        }

        @JvmStatic
        fun getAccounts(): List<AccountParameters> {
            return listOf(
                AccountParameters(
                    StandardAccount(
                        legacyDevnetAddressBook.accountAddress,
                        legacySigner,
                        legacyGatewayProvider,
                        cairoVersion = Felt.ZERO,
                    ),
                    legacyGatewayProvider,
                    legacyDevnetAddressBook,
                ),
                AccountParameters(
                    StandardAccount(
                        devnetAddressBook.accountAddress,
                        signer,
                        rpcProvider,
                        cairoVersion = Felt.ZERO,
                    ),
                    rpcProvider,
                    devnetAddressBook,
                ),
            )
        }

        @JvmStatic
        fun getAccountsLegacy(): List<AccountParameters> {
            return listOf(
                AccountParameters(
                    StandardAccount(
                        legacyDevnetAddressBook.accountAddress,
                        legacySigner,
                        legacyGatewayProvider,
                        cairoVersion = Felt.ZERO,
                    ),
                    legacyGatewayProvider,
                    legacyDevnetAddressBook,
                ),
                AccountParameters(
                    StandardAccount(
                        legacyDevnetAddressBook.accountAddress,
                        legacySigner,
                        legacyRpcProvider,
                        cairoVersion = Felt.ZERO,
                    ),
                    legacyRpcProvider,
                    legacyDevnetAddressBook,
                ),
            )
        }

        @JvmStatic
        @AfterAll
        fun after() {
            devnetClient.close()
            legacyDevnetClient.close()
        }
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `creating account with private key`(accountParameters: AccountParameters) {
        val provider = accountParameters.provider
        val privateKey = Felt(1234)
        StandardAccount(Felt.ZERO, privateKey, provider)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `get nonce test`(accountParameters: AccountParameters) {
        val account = accountParameters.account
        val nonce = account.getNonce().send()
        assert(nonce >= Felt.ZERO)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `get nonce twice`(accountParameters: AccountParameters) {
        val account = accountParameters.account
        val balanceContractAddress = accountParameters.addressBook.balanceContractAddress

        val startNonce = account.getNonce().send()
        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )
        account.execute(call).send()

        val endNonce = account.getNonce().send()
        assertEquals(
            startNonce.value + Felt.ONE.value,
            endNonce.value,
        )
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate fee for invoke transaction`(accountParameters: AccountParameters) {
        val account = accountParameters.account
        val balanceContractAddress = accountParameters.addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val request = account.estimateFee(call)
        val response = request.send()
        val feeEstimate = response.first()

        assertNotEquals(Felt.ZERO, feeEstimate.gasPrice)
        assertNotEquals(Felt.ZERO, feeEstimate.gasConsumed)
        assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
        assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
    }

    @ParameterizedTest
    @MethodSource("getAccountsLegacy")
    fun `estimate fee for declare v1 transaction`(accountParameters: AccountParameters) {
        val (account, provider, _) = accountParameters
        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x3b32bb615844ea7a9a56a8966af1a5ba1457b1f5c9162927ca1968975b0d2a9")
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

        val request = provider.getEstimateFee(listOf(signedTransaction.toPayload()), BlockTag.LATEST)
        val response = request.send()
        val feeEstimate = response.first()

        assertNotEquals(Felt.ZERO, feeEstimate.gasPrice)
        assertNotEquals(Felt.ZERO, feeEstimate.gasConsumed)
        assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
        assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
    }

    @Test
    fun `estimate message fee`() {
        val provider = rpcProvider
        val balanceContractAddress = devnetAddressBook.balanceContractAddress

        val message = MessageL1ToL2(
            fromAddress = Felt.fromHex("0xbe1259ff905cadbbaa62514388b71bdefb8aacc1"),
            toAddress = balanceContractAddress,
            selector = selectorFromName("increase_balance"),
            payload = listOf(
                Felt.fromHex("0x54d01e5fc6eb4e919ceaab6ab6af192e89d1beb4f29d916768c61a4d48e6c95"),
                Felt.fromHex("0x38d7ea4c68000"),
                Felt.fromHex("0x0"),
            ),
        )

        val request = provider.getEstimateMessageFee(
            message = message,
            blockTag = BlockTag.LATEST,
        )
        val response = request.send()

        assertNotEquals(Felt.ZERO, response.gasPrice)
        assertNotEquals(Felt.ZERO, response.gasConsumed)
        assertNotEquals(Felt.ZERO, response.overallFee)
        assertEquals(response.gasPrice.value.multiply(response.gasConsumed.value), response.overallFee.value)
    }

    // TODO: use getAccounts instead once declare v1 is fixed in devnet-rs
    @ParameterizedTest
    @MethodSource("getAccountsLegacy")
    fun `sign and send declare v1 transaction`(accountParameters: AccountParameters) {
        val (account, provider, _) = accountParameters

        val receiptProvider = when (provider) {
            is GatewayProvider -> provider
            is JsonRpcProvider -> MockUtils.mockUpdatedReceiptRpcProvider(provider)
            else -> throw IllegalStateException("Unknown provider type")
        }

        val contractCode = Path.of("src/test/resources/compiled_v0/providerTest.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x3b32bb615844ea7a9a56a8966af1a5ba1457b1f5c9162927ca1968975b0d2a9")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )

        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = receiptProvider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v2 transaction`(accountParameters: AccountParameters) {
        val (account, provider, _) = accountParameters

        val contractCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.json").readText()
        val casmCode = Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.casm").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            contractCasmDefinition,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign and send declare v2 transaction (cairo compiler v2)`(accountParameters: AccountParameters) {
        val (account, provider, _) = accountParameters

        val contractCode = Path.of("src/test/resources/compiled_v2/${provider::class.simpleName}_contract.json").readText()
        val casmCode = Path.of("src/test/resources/compiled_v2/${provider::class.simpleName}_contract.casm").readText()

        val contractDefinition = Cairo2ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            contractCasmDefinition,
            ExecutionParams(nonce, Felt(1000000000000000)),
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign single call test`(accountParameters: AccountParameters) {
        val (account, provider, addressBook) = accountParameters
        val balanceContractAddress = addressBook.balanceContractAddress

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

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign TypedData`(accountParameters: AccountParameters) {
        val (account, _, _) = accountParameters
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
    fun `execute single call`(accountParameters: AccountParameters) {
        val (account, provider, addressBook) = accountParameters
        val balanceContractAddress = addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val result = account.execute(call).send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute single call with specific fee`(accountParameters: AccountParameters) {
        // Note to future developers experiencing failures in this test:
        // This transaction may fail if the fee is too low.

        val (account, provider, addressBook) = accountParameters
        val balanceContractAddress = addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val maxFee = Felt(10000000000000000L)
        val result = account.execute(call, maxFee).send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
        assertNotEquals(Felt.ZERO, receipt.actualFee!!)
        // TODO: re-enable this once devnet-rs is fixed
        // assertTrue(receipt.actualFee!! < maxFee)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `sign multiple calls test`(accountParameters: AccountParameters) {
        val (account, provider, addressBook) = accountParameters
        val balanceContractAddress = addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val params = ExecutionParams(
            maxFee = Felt(1000000000000000),
            nonce = account.getNonce().send(),
        )

        val payload = account.sign(listOf(call, call, call), params)
        val response = provider.invokeFunction(payload).send()

        val receipt = provider.getTransactionReceipt(response.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `execute multiple calls`(accountParameters: AccountParameters) {
        val (account, provider, addressBook) = accountParameters
        val balanceContractAddress = addressBook.balanceContractAddress

        val call1 = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val call2 = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val result = account.execute(listOf(call1, call2)).send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `two executes with single call`(accountParameters: AccountParameters) {
        val (account, provider, addressBook) = accountParameters
        val balanceContractAddress = addressBook.balanceContractAddress

        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )

        val result = account.execute(call).send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()
        assertTrue(receipt.isAccepted)

        val call2 = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(20)),
        )

        val result2 = account.execute(call2).send()

        val receipt2 = provider.getTransactionReceipt(result2.transactionHash).send()
        assertTrue(receipt2.isAccepted)
    }

    @Test
    fun `cairo1 account calldata`() {
        val addressBook = devnetAddressBook
        val balanceContractAddress = addressBook.balanceContractAddress
        val accountAddress = addressBook.accountAddress

        val call1 = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10), Felt(20), Felt(30)),
        )

        val call2 = Call(
            contractAddress = Felt(999),
            entrypoint = "empty_calldata",
            calldata = listOf(),
        )

        val call3 = Call(
            contractAddress = Felt(123),
            entrypoint = "another_method",
            calldata = listOf(Felt(100), Felt(200)),
        )

        val account = StandardAccount(
            address = accountAddress,
            signer = signer,
            provider = rpcProvider,
            cairoVersion = Felt.ONE,
        )
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
    fun `estimate deploy account fee`(providerParameters: ProviderParameters) {
        val (provider, addressBook) = providerParameters
        val accountClassHash = addressBook.accountContractClassHash

        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountClassHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = accountClassHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            nonce = Felt.ZERO,
            forFeeEstimate = true,
        )
        assertEquals(
            payloadForFeeEstimation.version,
            Felt(BigInteger("340282366920938463463374607431768211457")),
        )

        val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
        assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `deploy account`(providerParameters: ProviderParameters) {
        val (provider, addressBook) = providerParameters
        val accountClassHash = addressBook.accountContractClassHash
        val balanceContractAddress = addressBook.balanceContractAddress

        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountClassHash,
            calldata = calldata,
            salt = salt,
        )
        legacyDevnetClient.prefundAccount(address)
        devnetClient.prefundAccount(address)

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payload = account.signDeployAccount(
            classHash = accountClassHash,
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
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )
        val result = account.execute(call).send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `send transaction signed for fee estimation`(providerParameters: ProviderParameters) {
        val (provider, addressBook) = providerParameters
        val accountClassHash = addressBook.accountContractClassHash

        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountClassHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = accountClassHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            nonce = Felt.ONE,
            forFeeEstimate = true,
        )
        assertEquals(
            payloadForFeeEstimation.version,
            Felt(BigInteger("340282366920938463463374607431768211457")),
        )

        assertThrows(RequestFailedException::class.java) {
            provider.deployAccount(payloadForFeeEstimation).send()
        }
    }

    @Test
    fun `simulate transactions`() {
        val addressBook = legacyDevnetAddressBook
        val accountAddress = addressBook.accountAddress
        val balanceContractAddress = addressBook.balanceContractAddress
        val accountClassHash = addressBook.accountContractClassHash

        val account = StandardAccount(accountAddress, legacySigner, legacyRpcProvider)

        val nonce = account.getNonce().send()
        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(1000)),
        )
        val params = ExecutionParams(
            nonce = nonce,
            maxFee = Felt(1000000000000000),
        )

        val invokeTx = account.sign(call, params)

        val privateKey = Felt(22222)
        val publicKey = StarknetCurve.getPublicKey(privateKey)
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
            legacyRpcProvider,
        )
        legacyDevnetClient.prefundAccount(address)
        val deployAccountTx = newAccount.signDeployAccount(
            classHash = accountClassHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.fromHex("0x11fcc58c7f7000"),
        )

        val simulationFlags = setOf<SimulationFlag>()
        val simulationResult = legacyRpcProvider.simulateTransactions(
            transactions = listOf(invokeTx, deployAccountTx),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(2, simulationResult.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)

        val invokeTxWithoutSignature = InvokeTransactionPayload(invokeTx.senderAddress, invokeTx.calldata, emptyList(), invokeTx.maxFee, invokeTx.version, invokeTx.nonce)
        val deployAccountTxWithoutSignature = DeployAccountTransactionPayload(deployAccountTx.classHash, deployAccountTx.salt, deployAccountTx.constructorCalldata, deployAccountTx.version, deployAccountTx.nonce, deployAccountTx.maxFee, emptyList())

        val simulationFlags2 = setOf(SimulationFlag.SKIP_VALIDATE)
        val simulationResult2 = legacyRpcProvider.simulateTransactions(
            transactions = listOf(invokeTxWithoutSignature, deployAccountTxWithoutSignature),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags2,
        ).send()

        assertEquals(2, simulationResult2.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)
    }
}
