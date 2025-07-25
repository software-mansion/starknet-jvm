package starknet.account

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.selectorFromName
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.toFelt
import com.swmansion.starknet.provider.exceptions.RequestFailedException
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
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
import java.time.Duration
import java.time.Instant
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
        private lateinit var legacyAccountAddress: Felt
        private lateinit var balanceContractAddress: Felt

        private lateinit var signer: Signer

        private lateinit var chainId: StarknetChainId
        private lateinit var account: Account
        private lateinit var secondAccount: Account

        @JvmStatic
        @BeforeAll
        fun before() {
            try {
                devnetClient.start()

                val accountDetails = devnetClient.createDeployAccount().details
                val legacyAccountDetails = devnetClient.createDeployAccount(classHash = DevnetClient.legacyAccountContractClassHash, accountName = "legacy_account").details
                balanceContractAddress = devnetClient.declareDeployContract("Balance", constructorCalldata = listOf(Felt(451))).contractAddress
                accountAddress = accountDetails.address
                legacyAccountAddress = legacyAccountDetails.address

                signer = StarkCurveSigner(accountDetails.privateKey)
                chainId = provider.getChainId().send()
                account = StandardAccount(
                    address = accountAddress,
                    signer = signer,
                    provider = provider,
                    chainId = chainId,
                )
                secondAccount = devnetClient.createDeployAccount(accountName = "second").details.let {
                    StandardAccount(
                        address = it.address,
                        provider = provider,
                        privateKey = it.privateKey,
                        chainId = chainId,
                    )
                }
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
    fun creatingAccountWithPrivateKey() {
        val address = Felt(123)
        val privateKey = Felt(456)
        val account = StandardAccount(address, privateKey, provider, chainId)
    }

    @Test
    fun `generate random private key`() {
        val randomPrivateKey = StandardAccount.generatePrivateKey()
        assertTrue(randomPrivateKey.value < Felt.PRIME)
        assertTrue(randomPrivateKey.hexStringPadded().length == 66)
    }

    @Test
    fun `cairo 0 account with automatic version detection`() {
        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10), Felt(20), Felt(30)),
        )
        val account = StandardAccount.create(
            address = legacyAccountAddress,
            signer = signer,
            provider = provider,
            chainId = chainId,
        )
        val params = InvokeParamsV3(Felt.ZERO, ResourceBoundsMapping.ZERO)

        val signedTx = account.signV3(call, params)

        val expectedCalldata = listOf(
            Felt(1),
            balanceContractAddress,
            selectorFromName("increase_balance"),
            Felt(0),
            Felt(3),
            Felt(3),
            Felt(10),
            Felt(20),
            Felt(30),
        )
        assertEquals(expectedCalldata, signedTx.calldata)

        val signedEmptyTx = account.signV3(listOf(), params)
        assertEquals(listOf(Felt.ZERO, Felt.ZERO), signedEmptyTx.calldata)
    }

    @Test
    fun createCairo1AccountWithAutomaticVersionDetection() {
        val call = Call(
            contractAddress = balanceContractAddress,
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10), Felt(20), Felt(30)),
        )

        val account = StandardAccount.create(
            address = accountAddress,
            signer = signer,
            provider = provider,
            chainId = chainId,
        )
        val params = InvokeParamsV3(Felt.ZERO, ResourceBoundsMapping.ZERO)
        val signedTx = account.signV3(call, params)

        val expectedCalldata = listOf(
            Felt(1),
            balanceContractAddress,
            selectorFromName("increase_balance"),
            Felt(3),
            Felt(10),
            Felt(20),
            Felt(30),
        )
        assertEquals(expectedCalldata, signedTx.calldata)

        val signedEmptyTx = account.signV3(listOf(), params)
        assertEquals(listOf(Felt.ZERO), signedEmptyTx.calldata)
    }

    @Nested
    inner class NonceTest {
        @Test
        fun getNonce() {
            val nonce = account.getNonce().send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun getNonceAtLatestBlockTag() {
            val nonce = account.getNonce(BlockTag.LATEST).send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun getNonceAtBlockHash() {
            val blockHashAndNumber = provider.getBlockHashAndNumber().send()

            val nonce = account.getNonce(blockHashAndNumber.blockHash).send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun getNonceAtBlockNumber() {
            val blockNumber = provider.getBlockNumber().send().value

            val nonce = account.getNonce(blockNumber).send()
            assert(nonce >= Felt.ZERO)
        }

        @Test
        fun `get nonce twice`() {
            val startNonce = account.getNonce().send()
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            account.executeV3(call, resourceBounds).send()

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
        fun estimateFeeForInvokeV1Transaction() {
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val request = account.estimateFeeV3(call)
            val feeEstimate = request.send().values.first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            val calculatedFee = feeEstimate.l1GasPrice.value * feeEstimate.l1GasConsumed.value + feeEstimate.l1DataGasPrice.value * feeEstimate.l1DataGasConsumed.value + feeEstimate.l2GasPrice.value * feeEstimate.l2GasConsumed.value
            assertEquals(
                calculatedFee,
                feeEstimate.overallFee.value,
            )
        }

        @Test
        fun estimateFeeForInvokeV3Transaction() {
            // docsStart
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val request = account.estimateFeeV3(
                listOf(call),
                skipValidate = false,
            )
            val feeEstimate = request.send().values.first()
            // docsEnd
            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            val calculatedFee = feeEstimate.l1GasPrice.value * feeEstimate.l1GasConsumed.value + feeEstimate.l1DataGasPrice.value * feeEstimate.l1DataGasConsumed.value + feeEstimate.l2GasPrice.value * feeEstimate.l2GasConsumed.value
            assertEquals(
                calculatedFee,
                feeEstimate.overallFee.value,
            )
        }

        @Test
        fun estimateFeeWithSkipValidateFlag() {
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val nonce = account.getNonce().send()
            val invokeTxV3Payload = account.signV3(
                call = call,
                params = InvokeParamsV3(
                    nonce = nonce,
                    resourceBounds = resourceBounds,
                ),
                forFeeEstimate = true,
            )
            assertEquals(TransactionVersion.V3_QUERY, invokeTxV3Payload.version)

            val invokeTxV3PayloadWithoutSignature = invokeTxV3Payload.copy(signature = emptyList())

            val request = provider.getEstimateFee(
                payload = listOf(invokeTxV3PayloadWithoutSignature),
                simulationFlags = setOf(SimulationFlagForEstimateFee.SKIP_VALIDATE),
            )

            val feeEstimates = request.send()
            println(feeEstimates)
            feeEstimates.values.forEach {
                assertNotEquals(Felt.ZERO, it.overallFee)

                val calculatedFee = it.l1GasPrice.value * it.l1GasConsumed.value + it.l1DataGasPrice.value * it.l1DataGasConsumed.value + it.l2GasPrice.value * it.l2GasConsumed.value
                assertEquals(
                    calculatedFee,
                    it.overallFee.value,
                )
            }
        }

        @Test
        fun estimateFeeForInvokeV3TransactionAtLatestBlockTag() {
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

            val request = account.estimateFeeV3(call, BlockTag.LATEST)
            val feeEstimate = request.send().values.first()

            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(
                feeEstimate.l1GasPrice.value * feeEstimate.l1GasConsumed.value + feeEstimate.l1DataGasPrice.value * feeEstimate.l1DataGasConsumed.value + feeEstimate.l2GasPrice.value * feeEstimate.l2GasConsumed.value,
                feeEstimate.overallFee.value,
            )
        }
    }

    @Nested
    inner class DeclareEstimateTest {
        @Test
        fun estimateFeeForDeclareV3Transaction() {
            // docsStart
            val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
            val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

            val contractDefinition = Cairo1ContractDefinition(contractCode)
            val contractCasmDefinition = CasmContractDefinition(casmCode)
            val nonce = account.getNonce().send()

            val params = DeclareParamsV3(
                nonce = nonce,
                resourceBounds = ResourceBoundsMapping.ZERO,
            )
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                contractCasmDefinition,
                params,
                true,
            )
            // docsEnd
            assertEquals(TransactionVersion.V3_QUERY, declareTransactionPayload.version)
            // docsStart
            val request = provider.getEstimateFee(payload = listOf(declareTransactionPayload), simulationFlags = emptySet())
            val feeEstimate = request.send().values.first()
            // docsEnd
            assertNotEquals(Felt.ZERO, feeEstimate.overallFee)
            assertEquals(
                feeEstimate.l1GasPrice.value * feeEstimate.l1GasConsumed.value + feeEstimate.l1DataGasPrice.value * feeEstimate.l1DataGasConsumed.value + feeEstimate.l2GasPrice.value * feeEstimate.l2GasConsumed.value,
                feeEstimate.overallFee.value,
            )
        }
    }

    @Test
    fun estimateMessageFee() {
        val l1l2ContractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_l1_l2.sierra.json").readText()
        val l1l2CasmContractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_l1_l2.casm.json").readText()

        val l1l2ContractDefinition = Cairo2ContractDefinition(l1l2ContractCode)
        val l1l2CasmContractDefinition = CasmContractDefinition(l1l2CasmContractCode)
        val nonce = account.getNonce().send()

        val resourceBounds = ResourceBoundsMapping(
            l1Gas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000),
                maxPricePerUnit = Uint128(10_000_000_000_000_000),
            ),
            l2Gas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000_000),
                maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
            ),
            l1DataGas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000),
                maxPricePerUnit = Uint128(10_000_000_000_000_000),
            ),
        )
        val declareTransactionPayload = account.signDeclareV3(
            l1l2ContractDefinition,
            l1l2CasmContractDefinition,
            DeclareParamsV3(nonce, resourceBounds),
        )
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

        assertNotEquals(Felt.ZERO, (response.l1GasPrice.value + response.l2GasPrice.value).toFelt)
        assertEquals(
            response.l1GasPrice.value * response.l1GasConsumed.value + response.l1DataGasPrice.value * response.l1DataGasConsumed.value,
            response.overallFee.value,
        )
    }

    @Nested
    inner class DeclareTest {
        @Test
        fun `sign and send declare v3 transaction (cairo compiler v2)`() {
            devnetClient.prefundAccountEth(accountAddress)

            val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_CounterContract.sierra.json").readText()
            val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_CounterContract.casm.json").readText()

            val contractDefinition = Cairo2ContractDefinition(contractCode)
            val contractCasmDefinition = CasmContractDefinition(casmCode)
            val nonce = account.getNonce().send()

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                contractCasmDefinition,
                DeclareParamsV3(nonce, resourceBounds),
            )
            val request = provider.declareContract(declareTransactionPayload)
            val result = request.send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun signAndSendDeclareV3Transaction() {
            devnetClient.prefundAccountStrk(accountAddress)
            // docsStart
            ScarbClient.buildSaltedContract(
                placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
                saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
            )
            val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
            val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

            val contractDefinition = Cairo2ContractDefinition(contractCode)
            val contractCasmDefinition = CasmContractDefinition(casmCode)
            val nonce = account.getNonce().send()

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val params = DeclareParamsV3(
                nonce = nonce,
                resourceBounds = resourceBounds,
            )
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                contractCasmDefinition,
                params,
            )
            val request = provider.declareContract(declareTransactionPayload)
            val result = request.send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            // docsEnd
            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `sign and send declare v3 transaction with tip`() {
            devnetClient.prefundAccountStrk(accountAddress)
            ScarbClient.buildSaltedContract(
                placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
                saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
            )
            val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
            val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

            val contractDefinition = Cairo2ContractDefinition(contractCode)
            val contractCasmDefinition = CasmContractDefinition(casmCode)
            val nonce = account.getNonce().send()

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )

            val tip = Uint64(12345)
            val params = DeclareParamsV3(
                nonce = nonce,
                resourceBounds = resourceBounds,
                tip = tip,
            )
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                contractCasmDefinition,
                params,
            )
            val request = provider.declareContract(declareTransactionPayload)
            val result = request.send()

            val tx = provider.getTransaction(result.transactionHash).send() as DeclareTransactionV3
            assertEquals(tip, tx.tip)

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            assertTrue(receipt.isAccepted)
        }
    }

    @Nested
    inner class SignTypedDataTest {
        private val tdRev0 by lazy { loadTypedData("rev_0/typed_data_struct_array_example.json") }
        private val tdRev1 by lazy { loadTypedData("rev_1/typed_data_basic_types_example.json") }

        @Test
        fun `sign TypedData revision 0`() {
            val typedData = tdRev0

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
        fun signTypedDataRevision1() {
            val typedData = tdRev1

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

            val typedData = tdRev0
            val signature = account.signTypedData(typedData)
            assertTrue(signature.isNotEmpty())

            val request = account.verifyTypedDataSignature(typedData, signature)
            assertThrows(RequestFailedException::class.java) {
                request.send()
            }
        }
    }

    @Nested
    inner class InvokeTest {
        @Test
        fun signV3SingleCall() {
            val call = Call(
                contractAddress = balanceContractAddress,
                calldata = listOf(Felt(10)),
                entrypoint = "increase_balance",
            )

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val params = InvokeParamsV3(
                nonce = account.getNonce().send(),
                resourceBounds = resourceBounds,
            )

            val payload = account.signV3(call, params)
            val request = provider.invokeFunction(payload)
            val response = request.send()

            val receipt = provider.getTransactionReceipt(response.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun executeV3SingleCall() {
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
        fun `execute v3 single call with tip`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val tip = Uint64(12345)
            val result = account.executeV3(call, tip).send()

            val tx = provider.getTransaction(result.transactionHash).send() as InvokeTransactionV3
            assertEquals(tip, tx.tip)

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `execute v3 single call with specific fee estimate multiplier`() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val result = account.executeV3(call).send()

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

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val result = account.executeV3(call, resourceBounds).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertTrue(receipt.isAccepted)
            assertNotEquals(Felt.ZERO, receipt.actualFee)
        }

        @Test
        fun signV3MultipleCalls() {
            val call = Call(
                contractAddress = balanceContractAddress,
                entrypoint = "increase_balance",
                calldata = listOf(Felt(10)),
            )

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val params = InvokeParamsV3(
                nonce = account.getNonce().send(),
                resourceBounds = resourceBounds,
            )

            val payload = account.signV3(listOf(call, call, call), params)
            val response = provider.invokeFunction(payload).send()

            val receipt = provider.getTransactionReceipt(response.transactionHash).send()

            assertTrue(receipt.isAccepted)
        }

        @Test
        fun executeV3MultipleCalls() {
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
        fun `execute v3 multiple calls with tip`() {
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

            val tip = Uint64(12345)
            val result = account.executeV3(listOf(call1, call2), tip).send()

            val tx = provider.getTransaction(result.transactionHash).send() as InvokeTransactionV3
            assertEquals(tip, tx.tip)

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            assertTrue(receipt.isAccepted)
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
            )
            val params = InvokeParamsV3(Felt.ZERO, ResourceBoundsMapping.ZERO)
            val signedTx = account.signV3(listOf(call1, call2, call3), params)

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

            val signedEmptyTx = account.signV3(listOf(), params)

            assertEquals(listOf(Felt.ZERO), signedEmptyTx.calldata)
        }
    }

    @Nested
    inner class DeployAccountEstimateTest {
        @Test
        fun estimateFeeForDeployAccountV3Transaction() {
            // docsStart
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
                resourceBounds = ResourceBoundsMapping.ZERO,
            )
            val payloadForFeeEstimation = account.signDeployAccountV3(
                classHash = accountContractClassHash,
                calldata = calldata,
                salt = salt,
                params = params,
                forFeeEstimate = true,
            )
            // docsEnd
            assertEquals(TransactionVersion.V3_QUERY, payloadForFeeEstimation.version)

            // docsStart
            val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
            // docsEnd
            assertTrue(feePayload.values.first().overallFee.value > Felt.ONE.value)
        }
    }

    @Nested
    inner class DeployAccountTest {
        @Test
        fun signAndSendDeployAccountV3Transaction() {
            // docsStart
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

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val params = DeployAccountParamsV3(
                resourceBounds = resourceBounds,
            )

            // Prefund the new account address with STRK
            // docsEnd
            devnetClient.prefundAccountStrk(address)
            // docsStart
            val payload = newAccount.signDeployAccountV3(
                classHash = accountContractClassHash,
                salt = salt,
                calldata = calldata,
                params = params,
                forFeeEstimate = false,
            )

            val response = provider.deployAccount(payload).send()
            // docsEnd
            // Make sure the address matches the calculated one
            assertEquals(address, response.address)
            // docsStart
            // Make sure tx matches what we sent
            val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV3
            // docsEnd
            assertEquals(payload.classHash, tx.classHash)
            assertEquals(payload.contractAddressSalt, tx.contractAddressSalt)
            assertEquals(payload.constructorCalldata, tx.constructorCalldata)
            assertEquals(payload.version, tx.version)
            assertEquals(payload.nonce, tx.nonce)
            assertEquals(payload.signature, tx.signature)

            // docsStart
            // Invoke function to make sure the account was deployed properly
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))
            val result = newAccount.executeV3(call).send()

            val receipt = provider.getTransactionReceipt(result.transactionHash).send()
            // docsEnd
            assertTrue(receipt.isAccepted)
        }

        @Test
        fun `sign and send deploy account v3 transaction with tip`() {
            val privateKey = Felt(22222)
            val publicKey = StarknetCurve.getPublicKey(privateKey)

            val salt = Felt(3)
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

            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )

            val tip = Uint64(12345)
            val params = DeployAccountParamsV3(
                resourceBounds = resourceBounds,
                tip = tip,
            )

            devnetClient.prefundAccountStrk(address)
            val payload = newAccount.signDeployAccountV3(
                classHash = accountContractClassHash,
                salt = salt,
                calldata = calldata,
                params = params,
                forFeeEstimate = false,
            )

            val response = provider.deployAccount(payload).send()
            assertEquals(address, response.address)

            val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV3
            assertEquals(payload.classHash, tx.classHash)
            assertEquals(payload.contractAddressSalt, tx.contractAddressSalt)
            assertEquals(payload.constructorCalldata, tx.constructorCalldata)
            assertEquals(payload.version, tx.version)
            assertEquals(payload.nonce, tx.nonce)
            assertEquals(payload.signature, tx.signature)
            assertEquals(payload.tip, tx.tip)

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
        val resourceBounds = ResourceBoundsMapping(
            l1Gas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000),
                maxPricePerUnit = Uint128(10_000_000_000_000_000),
            ),
            l2Gas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000_000),
                maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
            ),
            l1DataGas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000),
                maxPricePerUnit = Uint128(10_000_000_000_000_000),
            ),
        )
        val payloadForFeeEstimation = account.signDeployAccountV3(
            classHash = accountContractClassHash,
            salt = salt,
            calldata = calldata,
            resourceBounds = resourceBounds,
            forFeeEstimate = true,
        )
        assertEquals(TransactionVersion.V3_QUERY, payloadForFeeEstimation.version)

        assertThrows(RequestFailedException::class.java) {
            provider.deployAccount(payloadForFeeEstimation).send()
        }
    }

    @Nested
    inner class SimulateTransactionsTest {
        @Test
        fun simulateInvokeV3AndDeployAccountV3Transactions() {
            val account = StandardAccount(accountAddress, signer, provider, chainId)
            devnetClient.prefundAccountStrk(accountAddress)

            val nonce = account.getNonce().send()
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(1000)))
            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val params = InvokeParamsV3(
                nonce = nonce,
                resourceBounds = resourceBounds,
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
                calldata = calldata,
                salt = salt,
                resourceBounds = resourceBounds,
            )

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = provider.simulateTransactions(
                transactions = listOf(invokeTx, deployAccountTx),
                blockTag = BlockTag.PRE_CONFIRMED,
                simulationFlags = simulationFlags,
            ).send()
            assertEquals(2, simulationResult.values.size)
            assertTrue(simulationResult.values[0].transactionTrace is InvokeTransactionTraceBase)
            assertTrue(simulationResult.values[0].transactionTrace is InvokeTransactionTrace)
            assertTrue(simulationResult.values[1].transactionTrace is DeployAccountTransactionTrace)
        }

        @Test
        fun `simulate declare v3 transaction`() {
            devnetClient.prefundAccountStrk(accountAddress)

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
            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val declareTransactionPayload = account.signDeclareV3(
                contractDefinition,
                casmContractDefinition,
                DeclareParamsV3(
                    nonce = nonce,
                    resourceBounds = resourceBounds,
                ),
            )

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = provider.simulateTransactions(
                transactions = listOf(declareTransactionPayload),
                blockTag = BlockTag.PRE_CONFIRMED,
                simulationFlags = simulationFlags,
            ).send()
            assertEquals(1, simulationResult.values.size)
            val trace = simulationResult.values.first().transactionTrace
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
                            "l1_gas_consumed": "0x9d8",
                            "l1_gas_price": "0x3b9aca2f",
                            "l1_data_gas_consumed": "0x3a",
                            "l1_data_gas_price": "0x1a05",
                            "l2_gas_consumed": "0x9d8",
                            "l2_gas_price": "0x3b9aca2f",
                            "overall_fee": "0x24abbb63ea8",
                            "unit": "FRI"
                        },
                        "transaction_trace": {
                            "type": "INVOKE",
                            "execute_invocation": {
                               "revert_reason": "Placeholder revert reason."
                            },
                            "execution_resources": {
                                "l1_gas": "123",
                                "l1_data_gas": "456",
                                "l2_gas": "789"
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
            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(1000)))
            val resourceBounds = ResourceBoundsMapping(
                l1Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
                l2Gas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000_000),
                    maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
                ),
                l1DataGas = ResourceBounds(
                    maxAmount = Uint64(100_000_000_000),
                    maxPricePerUnit = Uint128(10_000_000_000_000_000),
                ),
            )
            val params = InvokeParamsV3(nonce, resourceBounds)
            val invokeTx = account.signV3(call, params)

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = mockProvider.simulateTransactions(
                transactions = listOf(invokeTx),
                blockTag = BlockTag.PRE_CONFIRMED,
                simulationFlags = simulationFlags,
            ).send()

            val trace = simulationResult.values.first().transactionTrace
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
                            "l1_gas_consumed": "0x9d8",
                            "l1_gas_price": "0x3b9aca2f",
                            "l1_data_gas_consumed": "0x3a",
                            "l1_data_gas_price": "0x1a05",
                            "l2_gas_consumed": "0x9d8",
                            "l2_gas_price": "0x3b9aca2f",
                            "l1_data_gas_consumed": "0x3a",
                            "overall_fee": "0x24abbb63ea8",
                            "unit": "FRI"
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
                                            "l1_gas": "123",
                                            "l2_gas": "789"
                                        },
                                        "is_reverted": false
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
                                    "l1_gas": "123",
                                    "l2_gas": "789"
                                },
                                "is_reverted": false
                            },
                            "execution_resources": {
                                "l1_gas": "123",
                                "l1_data_gas": "456",
                                "l2_gas": "789"
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

            val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(1000)))
            val params = InvokeParamsV3(nonce, ResourceBoundsMapping.ZERO)
            val invokeTx = account.signV3(call, params)

            val simulationFlags = setOf<SimulationFlag>()
            val simulationResult = mockProvider.simulateTransactions(
                transactions = listOf(invokeTx),
                blockTag = BlockTag.PRE_CONFIRMED,
                simulationFlags = simulationFlags,
            ).send()

            val trace = simulationResult.values.first().transactionTrace
            assertTrue(trace is InvokeTransactionTrace)
            val invokeTrace = trace as InvokeTransactionTrace
            val messages = invokeTrace.executeInvocation.messages
            assertEquals(2, messages.size)
            assertEquals(0, messages[0].order)
            assertEquals(1, messages[1].order)
        }
    }

    @Nested
    inner class OutsideExecutionTest {
        private val randomAddress = Felt.fromHex("0x123")

        @Test
        fun `isValidOutsideExecutionNonce returns true for not used nonce`() {
            val nonce = account.getOutsideExecutionNonce()
            assertEquals(
                true,
                account.isValidOutsideExecutionNonce(nonce).send(),
            )
        }

        @Test
        fun `isValidOutsideExecutionNonce returns false for used nonce`() {
            val call = createTransferCall(100)
            val now = Instant.now()
            val nonce = account.getOutsideExecutionNonce()

            val outsideCall = account.signOutsideExecutionCallV2(
                caller = secondAccount.address,
                executeAfter = Felt(now.minus(Duration.ofHours(1)).epochSecond),
                executeBefore = Felt(now.plus(Duration.ofHours(1)).epochSecond),
                calls = listOf(call),
                nonce = nonce,
            )

            secondAccount.executeV3(outsideCall).send()

            assertEquals(
                false,
                account.isValidOutsideExecutionNonce(nonce).send(),
            )
        }

        @Test
        fun `executes transfers from another specified account`() {
            val call1 = createTransferCall(100)
            val call2 = createTransferCall(200)
            val balanceBefore = getBalance(account.address)
            val now = Instant.now()
            val outsideCall = account.signOutsideExecutionCallV2(
                caller = secondAccount.address,
                executeAfter = Felt(now.minus(Duration.ofHours(1)).epochSecond),
                executeBefore = Felt(now.plus(Duration.ofHours(1)).epochSecond),
                calls = listOf(call1, call2),
            )

            secondAccount.executeV3(outsideCall).send()

            val balanceAfter = getBalance(account.address)
            val diff = balanceBefore - balanceAfter
            assertEquals(diff, 300.toBigInteger())
        }

        @Test
        fun `executes transfers from another any account`() {
            val call1 = createTransferCall(100)
            val call2 = createTransferCall(200)
            val balanceBefore = getBalance(account.address)
            val now = Instant.now()
            val outsideCall = account.signOutsideExecutionCallV2(
                caller = Felt.fromShortString("ANY_CALLER"),
                executeAfter = Felt(now.minus(Duration.ofHours(1)).epochSecond),
                executeBefore = Felt(now.plus(Duration.ofHours(1)).epochSecond),
                calls = listOf(call1, call2),
            )

            secondAccount.executeV3(outsideCall).send()

            val balanceAfter = getBalance(account.address)
            val diff = balanceBefore - balanceAfter
            assertEquals(diff, 300.toBigInteger())
        }

        @Test
        fun `executes transfers from the same account`() {
            val call1 = createTransferCall(100)
            val call2 = createTransferCall(200)
            val balanceBefore = getBalance(account.address)
            val now = Instant.now()
            val outsideCall = account.signOutsideExecutionCallV2(
                caller = account.address,
                executeAfter = Felt(now.minus(Duration.ofHours(1)).epochSecond),
                executeBefore = Felt(now.plus(Duration.ofHours(1)).epochSecond),
                calls = listOf(call1, call2),
            )

            account.executeV3(outsideCall).send()

            val balanceAfter = getBalance(account.address)
            val diff = balanceBefore - balanceAfter
            assertEquals(diff, 300.toBigInteger())
        }

        @Test
        fun `does not execute transfers signed to the different account`() {
            val call1 = createTransferCall(100)
            val call2 = createTransferCall(200)
            val balanceBefore = getBalance(account.address)
            val now = Instant.now()
            val outsideCall = account.signOutsideExecutionCallV2(
                caller = accountAddress,
                executeAfter = Felt(now.minus(Duration.ofHours(1)).epochSecond),
                executeBefore = Felt(now.plus(Duration.ofHours(1)).epochSecond),
                calls = listOf(call1, call2),
            )

            assertTrue {
                assertThrows<RpcRequestFailedException> {
                    secondAccount.executeV3(outsideCall).send()
                }.payload.contains("SRC9: invalid caller")
            }

            val balanceAfter = getBalance(account.address)
            assertEquals(balanceBefore, balanceAfter)
        }

        @Test
        fun `does not execute transfers after allowed time interval`() {
            val call1 = createTransferCall(100)
            val call2 = createTransferCall(200)
            val balanceBefore = getBalance(account.address)
            val now = Instant.now()
            val outsideCall = account.signOutsideExecutionCallV2(
                caller = secondAccount.address,
                executeAfter = Felt(now.minusSeconds(10).epochSecond),
                executeBefore = Felt(now.minusSeconds(1).epochSecond),
                calls = listOf(call1, call2),
            )

            assertTrue {
                assertThrows<RpcRequestFailedException> {
                    secondAccount.executeV3(outsideCall).send()
                }.payload.contains("SRC9: now >= execute_before")
            }

            val balanceAfter = getBalance(account.address)
            assertEquals(balanceBefore, balanceAfter)
        }

        private fun getBalance(address: Felt): BigInteger {
            return devnetClient.provider.callContract(
                Call(
                    contractAddress = DevnetClient.ethErc20ContractAddress,
                    entrypoint = "balance_of",
                    calldata = listOf(address),
                ),
            ).send()[0].value
        }

        private fun createTransferCall(amount: Int): Call {
            return Call(
                contractAddress = DevnetClient.ethErc20ContractAddress,
                entrypoint = "transfer",
                calldata = listOf(
                    randomAddress,
                    Uint256(amount).low,
                    Uint256(0).high,
                ),
            )
        }
    }
}
