package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import starknet.data.loadTypedData
import starknet.utils.DevnetClient
import starknet.utils.ScarbClient
import java.math.BigInteger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class StandardAccountTest {
    companion object {
        private val devnetClient = DevnetClient(
            port = 5051,
            accountDirectory = Paths.get("src/test/resources/accounts/standard_account_test"),
            contractsDirectory = Paths.get("src/test/resources/contracts"),
        )
        private val rpcUrl = devnetClient.rpcUrl
        private val provider = JsonRpcProvider(rpcUrl)

        private val accountContractClassHash = DevnetClient.accountContractClassHash
        private lateinit var accountAddress: Felt
        private lateinit var balanceContractAddress: Felt

        private lateinit var signer: Signer

        private lateinit var account: Account

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()

                val accountDetails = devnetClient.deployAccount("standard_account_test", prefund = true).details
                balanceContractAddress = devnetClient.declareDeployContract("Balance", constructorCalldata = listOf(Felt(451))).contractAddress
                accountAddress = accountDetails.address

                signer = StarkCurveSigner(accountDetails.privateKey)
                account = StandardAccount(
                    address = accountAddress,
                    signer = signer,
                    provider = provider,
                    cairoVersion = Felt.ZERO,
                )
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
    fun `creating account with private key`() {
        val privateKey = Felt(1234)
        StandardAccount(Felt.ZERO, privateKey, provider)
    }

    @Nested
    inner class NonceTest {
        @Test
        fun `get nonce`() {
            val nonce = account.getNonce().send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun `get nonce at latest block tag`() {
            val nonce = account.getNonce(BlockTag.LATEST).send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun `get nonce at block hash`() {
            val blockHashAndNumber = provider.getBlockHashAndNumber().send()

            val nonce = account.getNonce(blockHashAndNumber.blockHash).send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun `get nonce at block number`() {
            val blockNumber = provider.getBlockNumber().send()

            val nonce = account.getNonce(blockNumber).send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun `get nonce twice`() {
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
    }

    @Nested
    inner class InvokeEstimateTest {
        @Test
        fun `estimate fee for invoke v1 transaction`() {
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

        @Test
        fun `estimate fee for invoke v3 transaction`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val simulationFlags = emptySet<SimulationFlagForEstimateFee>()
            val request = account.estimateFeeV3(
                listOf(call),
                simulationFlags,
            )
            val response = request.send()
            val feeEstimate = response.first()

            assertNotEquals(Felt.ZERO, feeEstimate.gasPrice)
            assertNotEquals(Felt.ZERO, feeEstimate.gasConsumed)
            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }

        @Test
        fun `estimate fee for invoke transaction at latest block tag`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val request = account.estimateFee(call, BlockTag.LATEST)
            val response = request.send()
            val feeEstimate = response.first()

            assertNotEquals(Felt.ZERO, feeEstimate.gasPrice)
            assertNotEquals(Felt.ZERO, feeEstimate.gasConsumed)
            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }
    }

    @Test
    fun `estimate fee for declare v1 transaction`() {
        val contractCode = Path.of("src/test/resources/contracts_v0/target/release/balance.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test. Compiled contract format sometimes
        // changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        val classHash = Felt.fromHex("0x6d5c6e633015a1cb4637233f181a9bb9599be26ff16a8ce335822b41f98f70b")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000)),
        )

        val signedTransaction = TransactionFactory.makeDeclareV1Transaction(
            classHash = classHash,
            senderAddress = declareTransactionPayload.senderAddress,
            contractDefinition = declareTransactionPayload.contractDefinition,
            chainId = provider.getChainId().send(),
            nonce = nonce,
            maxFee = declareTransactionPayload.maxFee,
            signature = declareTransactionPayload.signature,
            version = declareTransactionPayload.version,
        )

        val request = provider.getEstimateFee(
            listOf(signedTransaction.toPayload()),
            BlockTag.LATEST,
            emptySet(),
        )
        val response = request.send()
        val feeEstimate = response.first()

        val txWithoutSignature = signedTransaction.copy(signature = emptyList())
        val request2 = provider.getEstimateFee(
            listOf(txWithoutSignature.toPayload()),
            BlockTag.LATEST,
            setOf(SimulationFlagForEstimateFee.SKIP_VALIDATE),
        )
        val response2 = request2.send()
        val feeEstimate2 = response2.first()

        listOf(feeEstimate, feeEstimate2).forEach {
            assertNotEquals(Felt.ZERO, it.gasPrice)
            assertNotEquals(Felt.ZERO, it.gasConsumed)
            assertNotEquals(Felt.ZERO, it.overallFee)
            assertEquals(it.gasPrice.value.multiply(it.gasConsumed.value), it.overallFee.value)
        }

        assertThrows(RequestFailedException::class.java) {
            provider.getEstimateFee(
                listOf(txWithoutSignature.toPayload()),
                BlockTag.LATEST,
                emptySet(),
            ).send()
        }
    }

    // TODO: Use message mocking instead of deploying l1l2 contract.
    //  This is planned for when Cairo 0 support is dropped, provided that devnet supports message mocking by then.
    @Test
    fun `estimate message fee`() {
        // Note to future developers experiencing failures in this test.
        // Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.

        val l1l2ContractCode = Path.of("src/test/resources/contracts_v0/target/release/l1l2.json").readText()
        val l1l2ContractDefinition = Cairo0ContractDefinition(l1l2ContractCode)
        val classHash = Felt.fromHex("0x310b77cf1190f2555fca715a990f9ff9f5c42e1b30b42cc3fdb573b8ab95fc1")
        val nonce = account.getNonce().send()
        val declareTransactionPayload = account.signDeclare(l1l2ContractDefinition, classHash, ExecutionParams(nonce, Felt(1000000000000000)))
        val l2ContractClassHash = provider.declareContract(declareTransactionPayload).send().classHash
        val l2ContractAddress = devnetClient.deployContract(
            classHash = l2ContractClassHash,
            constructorCalldata = listOf(),
        ).contractAddress

        val l1Address = Felt.fromHex("0x8359E4B0152ed5A731162D3c7B0D8D56edB165A0")
        val user = Felt.ONE
        val amount = Felt(1000)

        val message = MessageL1ToL2(
            fromAddress = l1Address,
            toAddress = l2ContractAddress,
            selector = selectorFromName("deposit"),
            payload = listOf(user, amount),
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

    @Test
    fun `sign and send declare v1 transaction`() {
        val contractCode = Path.of("src/test/resources/contracts_v0/target/release/balance.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x6d5c6e633015a1cb4637233f181a9bb9599be26ff16a8ce335822b41f98f70b")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(nonce, Felt(1000000000000000L)),
        )

        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `sign and send declare v2 transaction`() {
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

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

    @Test
    fun `sign and send declare v2 transaction (cairo compiler v2)`() {
        val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_CounterContract.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_CounterContract.casm.json").readText()

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

    @Test
    fun `sign and send declare v3 transaction`() {
        ScarbClient.createSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
        )
        ScarbClient.buildContracts(Path.of("src/test/resources/contracts_v2"))
        val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

        val contractDefinition = Cairo2ContractDefinition(contractCode)
        val contractCasmDefinition = CasmContractDefinition(casmCode)
        val nonce = account.getNonce().send()

        val params = DeclareParamsV3(
            nonce = nonce,
            l1ResourceBounds = ResourceBounds(
                maxAmount = Uint64(20000),
                maxPricePerUnit = Uint128(120000000000),
            ),
        )
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            contractCasmDefinition,
            params,
            false,
        )
        val request = provider.declareContract(declareTransactionPayload)
        val result = request.send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `sign TypedData`() {
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
        val provider = JsonRpcProvider(devnetClient.rpcUrl, httpService)
        val account = StandardAccount(Felt.ONE, Felt.ONE, provider)

        val typedData = loadTypedData("typed_data_struct_array_example.json")
        val signature = account.signTypedData(typedData)
        assertTrue(signature.isNotEmpty())

        val request = account.verifyTypedDataSignature(typedData, signature)
        assertThrows(RequestFailedException::class.java) {
            request.send()
        }
    }

    @Nested
    inner class InvokeTest {
        @Test
        fun `sign single call`() {
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

        @Test
        fun `sign v3 single call`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                calldata = listOf(Felt(10)),
                entrypoint = "increase_balance",
            )

            val params = ExecutionParamsV3(
                nonce = account.getNonce().send(),
                l1ResourceBounds = ResourceBounds(
                    maxAmount = Uint64(20000),
                    maxPricePerUnit = Uint128(120000000000),
                ),
            )

            val payload = account.sign(call, params)
            val request = provider.invokeFunction(payload)
            val response = request.send()

            val receipt = provider.getTransactionReceipt(response.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `execute single call`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val result = account.execute(call).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `execute v3 single call`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val result = account.executeV3(call).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `execute single call with specific fee`() {
            // Note to future developers experiencing failures in this test:
            // This transaction may fail if the fee is too low.
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val maxFee = Felt(10000000000000000L)
            val result = account.execute(call, maxFee).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
            assertNotEquals(Felt.ZERO, receipt.actualFee)
        }

        @Test
        fun `execute v3 single call with specific resource bounds`() {
            // Note to future developers experiencing failures in this test:
            // This transaction may fail if resource bounds are too low.
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val l1ResourceBounds = ResourceBounds(
                maxAmount = Uint64(20000),
                maxPricePerUnit = Uint128(120000000000),
            )
            val result = account.executeV3(call, l1ResourceBounds).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
            assertNotEquals(Felt.ZERO, receipt.actualFee)
        }

        @Test
        fun `sign multiple calls test`() {
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

        @Test
        fun `sign v3 multiple calls test`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val params = ExecutionParamsV3(
                nonce = account.getNonce().send(),
                l1ResourceBounds = ResourceBounds(
                    maxAmount = Uint64(20000),
                    maxPricePerUnit = Uint128(120000000000),
                ),
            )

            val payload = account.sign(listOf(call, call, call), params)
            val response = provider.invokeFunction(payload).send()

            val receipt = provider.getTransactionReceipt(response.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `execute multiple calls`() {
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

        @Test
        fun `execute v3 multiple calls`() {
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

            val result = account.executeV3(listOf(call1, call2)).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `two executes with single call`() {
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
        fun `two executes v3 with single call`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val result = account.executeV3(call).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            assertTrue(receipt.isAccepted)

            val call2 = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(20)),
            )

            val result2 = account.executeV3(call2).send()

            val receipt2 = provider.getTransactionReceipt(result2.transactionHash).send()
            assertTrue(receipt2.isAccepted)
        }

        @Test
        fun `cairo1 account calldata`() {
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
                provider = provider,
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
    }

    @Test
    fun `estimate deploy account v1 fee`() {
        val privateKey = Felt(11112)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountContractClassHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = accountContractClassHash,
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

    @Test
    fun `estimate deploy account v3 fee`() {
        val privateKey = Felt(22223)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt(2)
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountContractClassHash,
            calldata = calldata,
            salt = salt,
        )
        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val params = DeployAccountParamsV3(
            nonce = Felt.ZERO,
            l1ResourceBounds = ResourceBounds.ZERO,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = accountContractClassHash,
            salt = salt,
            calldata = calldata,
            params = params,
            forFeeEstimate = true,
        )

        val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
        assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
    }

    @Test
    fun `sing and send deploy account v1 transaction`() {
        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountContractClassHash,
            calldata = calldata,
            salt = salt,
        )
        devnetClient.prefundAccountEth(address)

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payload = account.signDeployAccount(
            classHash = accountContractClassHash,
            salt = salt,
            calldata = calldata,
            // 10*fee from estimate deploy account fee
            maxFee = Felt.fromHex("0x11fcc58c7f7000"),
        )

        val response = provider.deployAccount(payload).send()

        // Make sure the address matches the calculated one
        assertEquals(address, response.address)

        // Make sure tx matches what we sent
        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV1
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

    @Test
    fun `sign and send deploy account v3 transaction`() {
        val privateKey = Felt(22222)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt(2)
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountContractClassHash,
            calldata = calldata,
            salt = salt,
        )

        val newAccount = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val l1ResourceBounds = ResourceBounds(
            maxAmount = Uint64(20000),
            maxPricePerUnit = Uint128(120000000000),
        )
        val params = DeployAccountParamsV3(
            nonce = Felt.ZERO,
            l1ResourceBounds = l1ResourceBounds,
        )

        // Prefund the new account address with STRK
        devnetClient.prefundAccountStrk(address)

        val payload = newAccount.signDeployAccount(
            classHash = accountContractClassHash,
            salt = salt,
            calldata = calldata,
            params = params,
            forFeeEstimate = false,
        )

        val response = provider.deployAccount(payload).send()

        // Make sure the address matches the calculated one
        assertEquals(address, response.address)

        // Make sure tx matches what we sent
        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV3
        assertEquals(payload.classHash, tx.classHash)
        assertEquals(payload.salt, tx.contractAddressSalt)
        assertEquals(payload.constructorCalldata, tx.constructorCalldata)
        assertEquals(payload.version, tx.version)
        assertEquals(payload.nonce, tx.nonce)
        assertEquals(payload.signature, tx.signature)

        // Invoke function to make sure the account was deployed properly
        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )
        val result = newAccount.executeV3(call).send()

        val receipt = provider.getTransactionReceipt(result.transactionHash).send()

        assertTrue(receipt.isAccepted)
    }

    @Test
    fun `send transaction signed for fee estimation`() {
        val privateKey = Felt(11111)
        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val salt = Felt.ONE
        val calldata = listOf(publicKey)
        val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountContractClassHash,
            calldata = calldata,
            salt = salt,
        )

        val account = StandardAccount(
            address,
            privateKey,
            provider,
        )
        val payloadForFeeEstimation = account.signDeployAccount(
            classHash = accountContractClassHash,
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
    fun `simulate invoke and deploy account transactions`() {
        val account = StandardAccount(accountAddress, signer, provider)
        devnetClient.prefundAccountEth(accountAddress)

        val nonce = account.getNonce().send()
        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(1000)),
        )
        val params = ExecutionParams(nonce, Felt(5_482_000_000_000_00))

        val invokeTx = account.sign(call, params)

        val privateKey = Felt(22222)
        val publicKey = StarknetCurve.getPublicKey(privateKey)
        val salt = Felt.ONE
        val calldata = listOf(publicKey)

        val newAccountAddress = ContractAddressCalculator.calculateAddressFromHash(
            classHash = accountContractClassHash,
            calldata = calldata,
            salt = salt,
        )
        val newAccount = StandardAccount(
            newAccountAddress,
            privateKey,
            provider,
        )
        devnetClient.prefundAccountEth(newAccountAddress)
        val deployAccountTx = newAccount.signDeployAccount(
            classHash = accountContractClassHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt(4_482_000_000_000_00),
        )

        val simulationFlags = setOf<SimulationFlag>()
        val simulationResult = provider.simulateTransactions(
            transactions = listOf(invokeTx, deployAccountTx),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(2, simulationResult.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTraceBase)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)

        val invokeTxWithoutSignature = InvokeTransactionV1Payload(invokeTx.senderAddress, invokeTx.calldata, emptyList(), invokeTx.maxFee, invokeTx.version, invokeTx.nonce)
        val deployAccountTxWithoutSignature = DeployAccountTransactionV1Payload(deployAccountTx.classHash, deployAccountTx.salt, deployAccountTx.constructorCalldata, deployAccountTx.version, deployAccountTx.nonce, deployAccountTx.maxFee, emptyList())

        val simulationFlags2 = setOf(SimulationFlag.SKIP_VALIDATE)
        val simulationResult2 = provider.simulateTransactions(
            transactions = listOf(invokeTxWithoutSignature, deployAccountTxWithoutSignature),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags2,
        ).send()

        assertEquals(2, simulationResult2.size)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTraceBase)
        assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
        assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)
    }

    @Test
    fun `simulate declare v1 transaction`() {
        val contractCode = Path.of("src/test/resources/contracts_v0/target/release/balance.json").readText()
        val contractDefinition = Cairo0ContractDefinition(contractCode)
        val nonce = account.getNonce().send()

        // Note to future developers experiencing failures in this test.
        // 1. Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.
        // 2. If it fails on CI, make sure to delete the compiled contracts before running this test.
        // Chances are, the contract was compiled with a different compiler version.

        val classHash = Felt.fromHex("0x6d5c6e633015a1cb4637233f181a9bb9599be26ff16a8ce335822b41f98f70b")
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            classHash,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000L),
            ),
        )
        val simulationFlags = setOf<SimulationFlag>()
        val simulationResult = provider.simulateTransactions(
            transactions = listOf(declareTransactionPayload),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(1, simulationResult.size)
        val trace = simulationResult.first().transactionTrace
        assertTrue(trace is DeclareTransactionTrace)
    }

    @Test
    fun `simulate declare v2 transaction`() {
        ScarbClient.createSaltedContract(
            placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
            saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
        )
        ScarbClient.buildContracts(Path.of("src/test/resources/contracts_v1"))
        val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json").readText()
        val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json").readText()

        val contractDefinition = Cairo1ContractDefinition(contractCode)
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val nonce = account.getNonce().send()
        val declareTransactionPayload = account.signDeclare(
            contractDefinition,
            casmContractDefinition,
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000L),
            ),
        )

        val simulationFlags = setOf<SimulationFlag>()
        val simulationResult = provider.simulateTransactions(
            transactions = listOf(declareTransactionPayload),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()
        assertEquals(1, simulationResult.size)
        val trace = simulationResult.first().transactionTrace
        assertTrue(trace is DeclareTransactionTrace)
    }

    // TODO: replace this with a proper devnet test
    // Legacy devnet never returns invoke transaction trace that has revert_reason field in execution_invocation.
    @Test
    fun `simulate reverted invoke transaction`() {
        val mockedResponse = """
        {
            "jsonrpc": "2.0",
            "id": 0,
            "result": [
                {
                    "fee_estimation": {
                        "gas_consumed": "0x9d8",
                        "gas_price": "0x3b9aca2f",
                        "overall_fee": "0x24abbb63ea8"
                    },
                    "transaction_trace": {
                        "type": "INVOKE",
                        "execute_invocation": {
                           "revert_reason": "Placeholder revert reason."
                        }
                    }
                }
            ]
        }
        """.trimIndent()
        val httpService = mock<HttpService> {
            on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
        }
        val mockProvider = JsonRpcProvider(devnetClient.rpcUrl, httpService)

        val nonce = account.getNonce().send()
        val maxFee = Felt(1)
        val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(1000)))
        val params = ExecutionParams(nonce, maxFee)
        val invokeTx = account.sign(call, params)

        val simulationFlags = setOf<SimulationFlag>()
        val simulationResult = mockProvider.simulateTransactions(
            transactions = listOf(invokeTx),
            blockTag = BlockTag.LATEST,
            simulationFlags = simulationFlags,
        ).send()

        val trace = simulationResult.first().transactionTrace
        assertTrue(trace is InvokeTransactionTraceBase)
        assertTrue(trace is RevertedInvokeTransactionTrace)
        val revertedTrace = trace as RevertedInvokeTransactionTrace
        assertNotNull(revertedTrace.executeInvocation)
        assertNotNull(revertedTrace.executeInvocation.revertReason)
    }
}
