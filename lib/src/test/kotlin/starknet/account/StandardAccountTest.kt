package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.extensions.toFelt
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

        private lateinit var chainId: StarknetChainId
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
                chainId = provider.getChainId().send()
                account = StandardAccount(
                    address = accountAddress,
                    signer = signer,
                    provider = provider,
                    chainId = chainId,
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
        StandardAccount(Felt.ZERO, privateKey, provider, chainId)
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
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            account.executeV1(call).send()

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
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val request = account.estimateFeeV1(call)
            val feeEstimate = request.send().first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }

        @Test
        fun `estimate fee for invoke v3 transaction`() {
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val request = account.estimateFeeV3(
                listOf(call),
                skipValidate = false,
            )
            val feeEstimate = request.send().first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }

        @Test
        fun `estimate fee with skip validate flag`() {
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val nonce = account.getNonce().send()
            val invokeTxV1Payload = account.signV1(
                call = call,
                params = ExecutionParams(nonce, Felt.ZERO),
                forFeeEstimate = true,
            )
            val invokeTxV3Payload = account.signV3(
                call = call,
                params = InvokeParamsV3(nonce.value.add(BigInteger.ONE).toFelt, ResourceBounds.ZERO),
                forFeeEstimate = true,
            )
            assertEquals(Felt.fromHex("0x100000000000000000000000000000001"), invokeTxV1Payload.version.value)
            assertEquals(Felt.fromHex("0x100000000000000000000000000000003"), invokeTxV3Payload.version.value)

            val invokeTxV1PayloadWithoutSignature = invokeTxV1Payload.copy(signature = emptyList())
            val invokeTxV3PayloadWithoutSignature = invokeTxV3Payload.copy(signature = emptyList())

            val request = provider.getEstimateFee(
                payload = listOf(invokeTxV1PayloadWithoutSignature, invokeTxV3PayloadWithoutSignature),
                simulationFlags = setOf(SimulationFlagForEstimateFee.SKIP_VALIDATE),
            )

            val feeEstimates = request.send()
            feeEstimates.forEach {
                assertNotEquals(Felt.ZERO, it.overallFee)
                assertEquals(it.gasPrice.value.multiply(it.gasConsumed.value), it.overallFee.value)
            }
        }

        @Test
        fun `estimate fee for invoke v1 transaction at latest block tag`() {
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val request = account.estimateFeeV1(call, BlockTag.LATEST)
            val feeEstimate = request.send().first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }
    }

    @Nested
    inner class DeclareEstimateTest {
        @Test
        fun `estimate fee for declare v1 transaction`() {
            val contractCode = Path.of("src/test/resources/contracts_v0/target/release/balance.json").readText()
            val contractDefinition = Cairo0ContractDefinition(contractCode)
            val nonce = account.getNonce().send()

            // Note to future developers experiencing failures in this test. Compiled contract format sometimes
            // changes, this causes changes in the class hash.
            // If this test starts randomly falling, try recalculating class hash.
            val classHash = Felt.fromHex("0x6d5c6e633015a1cb4637233f181a9bb9599be26ff16a8ce335822b41f98f70b")
            val declareTransactionPayload = account.signDeclareV1(
                contractDefinition = contractDefinition,
                classHash = classHash,
                params = ExecutionParams(nonce, Felt.ZERO),
                forFeeEstimate = true,
            )

            assertEquals(Felt.fromHex("0x100000000000000000000000000000001"), declareTransactionPayload.version.value)

            val request = provider.getEstimateFee(payload = listOf(declareTransactionPayload), simulationFlags = emptySet())
            val feeEstimate = request.send().first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }

        @Test
        fun `estimate fee for declare v2 transaction`() {
            val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
            val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

            val contractDefinition = Cairo1ContractDefinition(contractCode)
            val contractCasmDefinition = CasmContractDefinition(casmCode)
            val nonce = account.getNonce().send()

            val declareTransactionPayload = account.signDeclareV2(
                sierraContractDefinition = contractDefinition,
                casmContractDefinition = contractCasmDefinition,
                params = ExecutionParams(nonce, Felt.ZERO),
                forFeeEstimate = true,
            )

            assertEquals(Felt.fromHex("0x100000000000000000000000000000002"), declareTransactionPayload.version.value)

            val request = provider.getEstimateFee(payload = listOf(declareTransactionPayload), simulationFlags = emptySet())
            val feeEstimate = request.send().first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value) }

        @Test
        fun `estimate fee for declare v3 transaction`() {
            val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
            val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

            val contractDefinition = Cairo1ContractDefinition(contractCode)
            val contractCasmDefinition = CasmContractDefinition(casmCode)
            val nonce = account.getNonce().send()

            val params = DeclareParamsV3(nonce = nonce, l1ResourceBounds = ResourceBounds.ZERO)
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                contractCasmDefinition,
                params,
                true,
            )

            assertEquals(Felt.fromHex("0x100000000000000000000000000000003"), declareTransactionPayload.version.value)

            val request = provider.getEstimateFee(payload = listOf(declareTransactionPayload), simulationFlags = emptySet())
            val feeEstimate = request.send().first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(feeEstimate.gasPrice.value.multiply(feeEstimate.gasConsumed.value), feeEstimate.overallFee.value)
        }
    }

    @Test
    fun `estimate message fee`() {
        // Note to future developers experiencing failures in this test.
        // Compiled contract format sometimes changes, this causes changes in the class hash.
        // If this test starts randomly falling, try recalculating class hash.

        // TODO: Migrate l1l2 contract to Cairo 1.
        val l1l2ContractCode = Path.of("src/test/resources/contracts_v0/target/release/l1l2.json").readText()
        val l1l2ContractDefinition = Cairo0ContractDefinition(l1l2ContractCode)
        val classHash = Felt.fromHex("0x310b77cf1190f2555fca715a990f9ff9f5c42e1b30b42cc3fdb573b8ab95fc1")
        val nonce = account.getNonce().send()
        val declareTransactionPayload = account.signDeclareV1(l1l2ContractDefinition, classHash, ExecutionParams(nonce, Felt(1000000000000000)))
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

    @Nested
    inner class DeclareTest {
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
            val declareTransactionPayload = account.signDeclareV1(
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

            val declareTransactionPayload = account.signDeclareV2(
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

            val declareTransactionPayload = account.signDeclareV2(
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
            ScarbClient.buildSaltedContract(
                placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
                saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
            )
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
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                contractCasmDefinition,
                params,
            )
            val request = provider.declareContract(declareTransactionPayload)
            val result = request.send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }
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
        val account = StandardAccount(Felt.ONE, Felt.ONE, provider, chainId)

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

            val payload = account.signV1(call, params)
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

            val params = InvokeParamsV3(
                nonce = account.getNonce().send(),
                l1ResourceBounds = ResourceBounds(
                    maxAmount = Uint64(20000),
                    maxPricePerUnit = Uint128(120000000000),
                ),
            )

            val payload = account.signV3(call, params)
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

            val result = account.executeV1(call).send()

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
            val result = account.executeV1(call, maxFee).send()

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

            val payload = account.signV1(listOf(call, call, call), params)
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

            val params = InvokeParamsV3(
                nonce = account.getNonce().send(),
                l1ResourceBounds = ResourceBounds(
                    maxAmount = Uint64(20000),
                    maxPricePerUnit = Uint128(120000000000),
                ),
            )

            val payload = account.signV3(listOf(call, call, call), params)
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

            val result = account.executeV1(listOf(call1, call2)).send()

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

            val result = account.executeV1(call).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            assertTrue(receipt.isAccepted)

            val call2 = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(20)),
            )

            val result2 = account.executeV1(call2).send()

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
                chainId = chainId,
                cairoVersion = Felt.ONE,
            )
            val params = ExecutionParams(Felt.ZERO, Felt.ZERO)
            val signedTx = account.signV1(listOf(call1, call2, call3), params)

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

            val signedEmptyTx = account.signV1(listOf(), params)

            assertEquals(listOf(Felt.ZERO), signedEmptyTx.calldata)
        }
    }

    @Nested
    inner class DeployAccountEstimateTest {
        @Test
        fun `estimate fee for deploy account v1 transaction`() {
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
                chainId,
            )
            val payloadForFeeEstimation = account.signDeployAccountV1(
                classHash = accountContractClassHash,
                salt = salt,
                calldata = calldata,
                maxFee = Felt.ZERO,
                nonce = Felt.ZERO,
                forFeeEstimate = true,
            )
            assertEquals(Felt.fromHex("0x100000000000000000000000000000001"), payloadForFeeEstimation.version.value)

            val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
            assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
        }

        @Test
        fun `estimate fee for deploy account v3 transaction`() {
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
                chainId,
            )
            val params = DeployAccountParamsV3(
                nonce = Felt.ZERO,
                l1ResourceBounds = ResourceBounds.ZERO,
            )
            val payloadForFeeEstimation = account.signDeployAccountV3(
                classHash = accountContractClassHash,
                salt = salt,
                calldata = calldata,
                params = params,
                forFeeEstimate = true,
            )

            assertEquals(Felt.fromHex("0x100000000000000000000000000000003"), payloadForFeeEstimation.version.value)

            val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
            assertTrue(feePayload.first().overallFee.value > Felt.ONE.value)
        }
    }

    @Nested
    inner class DeployAccountTest {
        @Test
        fun `sign and send deploy account v1 transaction`() {
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
                chainId,
            )
            val payload = account.signDeployAccountV1(
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
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))
            val result = account.executeV1(call).send()

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
                chainId,
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

            val payload = newAccount.signDeployAccountV3(
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
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))
            val result = newAccount.executeV3(call).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }
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
            chainId,
        )
        val payloadForFeeEstimation = account.signDeployAccountV1(
            classHash = accountContractClassHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            nonce = Felt.ONE,
            forFeeEstimate = true,
        )
        assertEquals(Felt.fromHex("0x100000000000000000000000000000001"), payloadForFeeEstimation.version.value)

        assertThrows(RequestFailedException::class.java) {
            provider.deployAccount(payloadForFeeEstimation).send()
        }
    }

    @Nested
    inner class SimulateTransactionsTest {
        @Test
        fun `simulate invoke v1 and deploy account v1 transactions`() {
            val account = StandardAccount(accountAddress, signer, provider, chainId)
            devnetClient.prefundAccountEth(accountAddress)

            val nonce = account.getNonce().send()
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(1000)),
            )
            val params = ExecutionParams(nonce, Felt(5_482_000_000_000_00))

            val invokeTx = account.signV1(call, params)

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
                chainId,
            )
            devnetClient.prefundAccountEth(newAccountAddress)
            val deployAccountTx = newAccount.signDeployAccountV1(
                classHash = accountContractClassHash,
                salt = salt,
                calldata = calldata,
                maxFee = Felt(4_482_000_000_000_00),
            )

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = provider.simulateTransactions(
                transactions = listOf(invokeTx, deployAccountTx),
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags,
            ).send()
            assertEquals(2, simulationResult.size)
            assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTraceBase)
            assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
            assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)

            val invokeTxWithoutSignature = invokeTx.copy(signature = emptyList())
            val deployAccountTxWithoutSignature = deployAccountTx.copy(signature = emptyList())

            val simulationFlags2 = setOf(SimulationFlag.SKIP_VALIDATE)
            val simulationResult2 = provider.simulateTransactions(
                transactions = listOf(invokeTxWithoutSignature, deployAccountTxWithoutSignature),
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags2,
            ).send()

            assertEquals(2, simulationResult2.size)
            assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTraceBase)
            assertTrue(simulationResult[0].transactionTrace is InvokeTransactionTrace)
            assertTrue(simulationResult[1].transactionTrace is DeployAccountTransactionTrace)
        }

        @Test
        fun `simulate invoke v3 and deploy account v3 transactions`() {
            val account = StandardAccount(accountAddress, signer, provider, chainId)
            devnetClient.prefundAccountStrk(accountAddress)

            val nonce = account.getNonce().send()
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(1000)))
            val params = InvokeParamsV3(
                nonce = nonce,
                l1ResourceBounds = ResourceBounds(
                    maxAmount = Uint64(20000),
                    maxPricePerUnit = Uint128(120000000000),
                ),
            )

            val invokeTx = account.signV3(call, params)

            val privateKey = Felt(22222)
            val publicKey = StarknetCurve.getPublicKey(privateKey)
            val salt = Felt.ONE
            val calldata = listOf(publicKey)

            val newAccountAddress = ContractAddressCalculator.calculateAddressFromHash(
                classHash = accountContractClassHash,
                calldata = calldata,
                salt = salt,
            )
            val newAccount = StandardAccount(newAccountAddress, privateKey, provider, chainId)

            devnetClient.prefundAccountStrk(newAccountAddress)
            val deployAccountTx = newAccount.signDeployAccountV3(
                classHash = accountContractClassHash,
                salt = salt,
                calldata = calldata,
                l1ResourceBounds = ResourceBounds(
                    maxAmount = Uint64(20000),
                    maxPricePerUnit = Uint128(120000000000),
                ),
            )

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = provider.simulateTransactions(
                transactions = listOf(invokeTx, deployAccountTx),
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags,
            ).send()
            assertEquals(2, simulationResult.size)
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
            val declareTransactionPayload = account.signDeclareV1(
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
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags,
            ).send()
            assertEquals(1, simulationResult.size)
            val trace = simulationResult.first().transactionTrace
            assertTrue(trace is DeclareTransactionTrace)
        }

        @Test
        fun `simulate declare v2 transaction`() {
            ScarbClient.buildSaltedContract(
                placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
                saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
            )
            val contractCode =
                Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json")
                    .readText()
            val casmCode =
                Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json")
                    .readText()

            val contractDefinition = Cairo1ContractDefinition(contractCode)
            val casmContractDefinition = CasmContractDefinition(casmCode)

            val nonce = account.getNonce().send()
            val declareTransactionPayload = account.signDeclareV2(
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
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags,
            ).send()
            assertEquals(1, simulationResult.size)
            val trace = simulationResult.first().transactionTrace
            assertTrue(trace is DeclareTransactionTrace)
        }

        @Test
        fun `simulate declare v3 transaction`() {
            ScarbClient.buildSaltedContract(
                placeholderContractPath = Path.of("src/test/resources/contracts_v1/src/placeholder_hello_starknet.cairo"),
                saltedContractPath = Path.of("src/test/resources/contracts_v1/src/salted_hello_starknet.cairo"),
            )
            val contractCode =
                Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.sierra.json")
                    .readText()
            val casmCode =
                Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_SaltedHelloStarknet.casm.json")
                    .readText()

            val contractDefinition = Cairo1ContractDefinition(contractCode)
            val casmContractDefinition = CasmContractDefinition(casmCode)

            val nonce = account.getNonce().send()
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                casmContractDefinition,
                DeclareParamsV3(
                    nonce = nonce,
                    l1ResourceBounds = ResourceBounds(
                        maxAmount = Uint64(20000),
                        maxPricePerUnit = Uint128(120000000000),
                    ),
                ),
            )

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = provider.simulateTransactions(
                transactions = listOf(declareTransactionPayload),
                blockTag = BlockTag.PENDING,
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
            val invokeTx = account.signV1(call, params)

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = mockProvider.simulateTransactions(
                transactions = listOf(invokeTx),
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags,
            ).send()

            val trace = simulationResult.first().transactionTrace
            assertTrue(trace is InvokeTransactionTraceBase)
            assertTrue(trace is RevertedInvokeTransactionTrace)
            val revertedTrace = trace as RevertedInvokeTransactionTrace
            assertNotNull(revertedTrace.executeInvocation)
            assertNotNull(revertedTrace.executeInvocation.revertReason)
        }

        @Test
        fun `simulate transaction with messages`() {
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
                                "contract_address": "0x4428a52af4b56b60eafba3bfe8d45f06b3ba6567db259e1f815f818632fd18f",
                                "entry_point_selector": "0x15d40a3d6ca2ac30f4031e42be28da9b056fef9bb7357ac5e85627ee876e5ad",
                                "calldata": [
                                    "0x1",
                                    "0x2"
                                ],
                                "caller_address": "0x0",
                                "class_hash": "0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f",
                                "entry_point_type": "EXTERNAL",
                                "call_type": "CALL",
                                "result": [
                                    "0x1",
                                    "0x1"
                                ],
                                "calls": [
                                    {
                                        "contract_address": "0x49d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7",
                                        "entry_point_selector": "0x83afd3f4caedc6eebf44246fe54e38c95e3179a5ec9ea81740eca5b482d12e",
                                        "calldata": [
                                            "0x1",
                                            "0x3e8",
                                            "0x0"
                                        ],
                                        "caller_address": "0x4428a52af4b56b60eafba3bfe8d45f06b3ba6567db259e1f815f818632fd18f",
                                        "class_hash": "0x6a22bf63c7bc07effa39a25dfbd21523d211db0100a0afd054d172b81840eaf",
                                        "entry_point_type": "EXTERNAL",
                                        "call_type": "CALL",
                                        "result": [
                                            "0x1"
                                        ],
                                        "calls": [],
                                        "events": [
                                            {
                                                "keys": [
                                                    "0x99cd8bde557814842a3121e8ddfd433a539b8c9f14bf31ebf108d12e6196e9"
                                                ],
                                                "data": [
                                                    "0x4428a52af4b56b60eafba3bfe8d45f06b3ba6567db259e1f815f818632fd18f",
                                                    "0x1"
                                                ],
                                                "order": 0
                                            }
                                        ],
                                        "messages": [],
                                        "execution_resources": {
                                            "steps": 582
                                        }
                                    }
                                ],
                                "events": [],
                                "messages": [
                                    {
                                        "order": 0,
                                        "from_address": "0x123",
                                        "to_address": "0x456",
                                        "payload": ["0x1", "0x2"]
                                    }, 
                                    {
                                        "order": 1,
                                        "from_address": "0x456",
                                        "to_address": "0x789",
                                        "payload": []
                                    }
                                ],
                                "execution_resources": {
                                    "steps": 800
                                }
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
            val invokeTx = account.signV1(call, params)

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = mockProvider.simulateTransactions(
                transactions = listOf(invokeTx),
                blockTag = BlockTag.PENDING,
                simulationFlags = simulationFlags,
            ).send()

            val trace = simulationResult.first().transactionTrace
            assertTrue(trace is InvokeTransactionTrace)
            val invokeTrace = trace as InvokeTransactionTrace
            val messages = invokeTrace.executeInvocation.messages
            assertEquals(2, messages.size)
            assertEquals(0, messages[0].order)
            assertEquals(1, messages[1].order)
        }
    }
}
