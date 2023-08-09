package integration.getters

import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.OkHttpService
import com.swmansion.starknet.signer.Signer
import com.swmansion.starknet.signer.StarkCurveSigner
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.spy
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText

@Execution(ExecutionMode.SAME_THREAD)
class GettersTest {
    @Serializable
    data class Config(
        @JsonNames("rpc_url")
        val rpcUrl: String,

        @JsonNames("gateway_url")
        val gatewayUrl: String = "https://external.integration.starknet.io/gateway",

        @JsonNames("feeder_gateway_url")
        val feederGatewayUrl: String = "https://external.integration.starknet.io/feeder_gateway",

        @JsonNames("integration_account_address")
        val integrationAccountAddress: Felt,

        @JsonNames("private_key")
        val privateKey: Felt,
    )

    companion object {
        @JvmStatic
//        private val config: Config = Json { ignoreUnknownKeys = true }
//                .decodeFromString(Config.serializer(), Path.of("src/test/kotlin/integration/config.json").readText())
        // TODO: move this to init before all

        private val json = Json { ignoreUnknownKeys = true }

        //        private lateinit var rpcUrl: String
//        private lateinit var gatewayUrl: String
//        private lateinit var feederGatewayUrl: String
//
//        private lateinit var privateKey: Felt
// //        private val deployAccountSalt = Felt(789)
//
        private lateinit var signer: Signer

        //        private lateinit var integrationAccountAddress: Felt
//
        private lateinit var gatewayProvider: GatewayProvider
        private lateinit var rpcProvider: JsonRpcProvider

        //
// //        private lateinit var balanceContractAddress: Felt
        private lateinit var accountAddress: Felt

        //        private lateinit var accountClassHash: Felt
        private val config: Config?
            get() {
                val configPath = Paths.get("src/test/kotlin/integration/config.json")
                val configSource = System.getenv("STARKNET_JVM_INTEGRATION_TEST_SOURCE")?.lowercase()
                    ?: "file"

                return when (configSource) {
                    "env" -> null
                    "file" -> {
                        if (!Files.exists(configPath)) {
                            throw IllegalStateException("Config file not found")
                        }
                        json.decodeFromString(Config.serializer(), configPath.readText())
                    }
                    else -> throw IllegalStateException("Invalid configuration source provided")
                }
            }

        private val rpcUrl: String
            get() {
                return config?.rpcUrl
                    ?: System.getenv("STARKNET_JVM_RPC_URL")
                    ?: throw RuntimeException("RPC_URL not found in config file or environment variable")
            }

        private val gatewayUrl: String
            get() {
                return config?.gatewayUrl
                    ?: System.getenv("STARKNET_JVM_GATEWAY_URL")
                    ?: "https://external.integration.starknet.io/gateway"
            }

        private val feederGatewayUrl: String
            get() {
                return config?.feederGatewayUrl
                    ?: System.getenv("STARKNET_JVM_FEEDER_GATEWAY_URL")
                    ?: "https://external.integration.starknet.io/feeder_gateway"
            }

        private val integrationAccountAddress: Felt
            get() {
                val address = config?.integrationAccountAddress?.hexString()
                    ?: System.getenv("STARKNET_JVM_INTEGRATION_ACCOUNT_ADDRESS")
                    ?: throw RuntimeException("INTEGRATION_ACCOUNT_ADDRESS not found in config file or environment variable")
                return Felt.fromHex(address)
            }

        private val privateKey: Felt
            get() {
                val key = config?.privateKey?.hexString()
                    ?: System.getenv("STARKNET_JVM_PRIVATE_KEY")
                    ?: throw RuntimeException("PRIVATE_KEY not found in config file or environment variable")
                return Felt.fromHex(key)
            }

        @JvmStatic
        @BeforeAll
        fun before() {
//            val config = json.decodeFromString(Config.serializer(), Path.of("src/test/kotlin/integration/config.json").readText())
//            rpcUrl = config.rpcUrl
//            gatewayUrl = config.gatewayUrl
//            feederGatewayUrl = config.feederGatewayUrl
//            integrationAccountAddress = config.integrationAccountAddress
//            privateKey = config.privateKey

            signer = StarkCurveSigner(
                privateKey = privateKey,
            )
            gatewayProvider = GatewayProvider(
                feederGatewayUrl,
                gatewayUrl,
                StarknetChainId.TESTNET,
            )
            rpcProvider = JsonRpcProvider(
                rpcUrl,
                StarknetChainId.TESTNET,
            )

//                val (deployAddress, _) = devnetClient.deployContract(Path.of("src/test/resources/compiled_v0/providerTest.json"))
//                balanceContractAddress = deployAddress

//                deployAccount()
            accountAddress = integrationAccountAddress
        }

//        private fun deployAccount() {
//            val contractDeployer = ContractDeployer.deployInstance(devnetClient)
//            val (classHash, _) = devnetClient.declareContract(Path.of("src/test/resources/compiled_v0/account.json"))
//            accountClassHash = classHash
//            accountAddress = contractDeployer.deployContract(classHash, calldata = listOf(signer.publicKey))
//            devnetClient.prefundAccount(accountAddress)
//        }

        private fun deployContract(filePath: String) {
            val file = Path.of(filePath)
            // Declare contract using JsonRpcProvider
            val (account, provider) = AccountAndProvider(
                StandardAccount(
                    accountAddress,
                    privateKey,
                    rpcProvider,
                ),
                rpcProvider,
            )

            val contractCode = Path.of(filePath).readText()
            val contractDefinition = Cairo0ContractDefinition(contractCode)
            val nonce = account.getNonce().send()

            // Note to future developers experiencing failures in this test. Compiled contract format sometimes
            // changes, this causes changes in the class hash.
            // If this test starts randomly falling, try recalculating class hash.
            val classHash = Felt.fromHex("0x37475b8cd1e7360416bae6bc332ba4bc50936cd2d0f9f2207507acbac172e8d")
            val declareTransactionPayload = account.signDeclare(
                contractDefinition,
                classHash,
                ExecutionParams(nonce, Felt(1000000000000000)),
            )

            val request: Request<DeclareResponse> = provider.declareContract(declareTransactionPayload)
            val result = request.send()
            val receipt = provider.getTransactionReceipt(result.transactionHash).send()

            assertNotNull(result)
            assertNotNull(receipt)
            assertTrue(receipt.isAccepted)
        }

        data class AccountAndProvider(val account: Account, val provider: Provider)

        @JvmStatic
        private fun getProviders(): List<Provider> = listOf(
            gatewayProvider,
            rpcProvider,
        )

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
        fun after() {}
    }

    @Test
    fun `estimate message fee`() {
        val provider = rpcProvider

        val gasConsumed = Felt(19931)
        val gasPrice = Felt(1022979559)
        val overallFee = Felt(20389005590429)

        val message = RpcMessageL1ToL2(
            fromAddress = Felt.fromHex("0xbe1259ff905cadbbaa62514388b71bdefb8aacc1"),
            toAddress = Felt.fromHex("0x073314940630fd6dcda0d772d4c972c4e0a9946bef9dabf4ef84eda8ef542b82"),
            selector = Felt.fromHex("0x02d757788a8d8d6f21d1cd40bce38a8222d70654214e96ff95d8086e684fbee5"),
            payload = listOf(
                Felt.fromHex("0x54d01e5fc6eb4e919ceaab6ab6af192e89d1beb4f29d916768c61a4d48e6c95"),
                Felt.fromHex("0x38d7ea4c68000"),
                Felt.fromHex("0x0"),
            ),
        )

        val request = provider.getEstimateMessageFee(
            message = message,
            blockNumber = 306687,
        )
        val response = request.send()

        assertNotNull(response)
        assertNotNull(response.gasConsumed)
        assertNotNull(response.gasPrice)
        assertNotNull(response.overallFee)

        assertEquals(gasPrice, response.gasPrice)
        assertEquals(gasConsumed, response.gasConsumed)
        assertEquals(overallFee, response.overallFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy account transaction and receipt`(provider: Provider) {
        val transactionHash = Felt.fromHex("0x029da9f8997ce580718fa02ed0bd628976418b30a0c5c542510aaef21a4445e4")
        val tx = provider.getTransaction(transactionHash).send()

        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(Felt(10000000000000000L), tx.maxFee)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
            }
            is JsonRpcProvider -> {
                receipt = receipt as DeployRpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get reverted invoke transaction and receipt`(provider: Provider) {
        val transactionHash = Felt.fromHex("0x5e2e61a59e3f254f2c65109344be985dff979abd01b9c15b659a95f466689bf")
        val tx = provider.getTransaction(transactionHash).send()
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertFalse(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.REVERTED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNotNull(receipt.revertReason)
        //        println(receipt.revertReason)

        if (provider is GatewayProvider) {
            receipt = receipt as GatewayTransactionReceipt

            assertEquals(TransactionStatus.REVERTED, receipt.status)
        } else if (provider is JsonRpcProvider) {
            receipt = receipt as RpcTransactionReceipt
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get invoke transaction with events`(provider: Provider) {
        val transactionHash = Felt.fromHex("0x34223514e92989608e3b36f2a2a53011fa0699a275d7936a18921a11963c792")
        val tx = provider.getTransaction(transactionHash).send()
        assertNotNull(tx)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(4, receipt.events.size)
        val firstEvent = receipt.events[0]
        assertEquals(
            listOf(Felt.fromHex("0x07c930a86c2ed72bea4767b688367e06fd2b2a58915bdd3cfa16fb61b485e8c5")),
            firstEvent.data,
        )
        assertEquals(
            Felt.fromHex("0x05ee5dbac8c39fe9ef8d7761cc84086949d7dc42dd6233cb6310208272ee87ea"),
            firstEvent.address,
        )

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
            }
            is JsonRpcProvider -> {
                receipt = receipt as RpcTransactionReceipt
            }
        }
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v1 transaction and receipt`(provider: Provider) {
        val transactionHash = Felt.fromHex("0x0417ec8ece9d2d2e68307069fdcde3c1fd8b0713b8a2687b56c19455c6ea85c1")
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV1
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertNotNull(tx.classHash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(
            Felt.fromHex("0x043403d83a5efac5193d8942b135fbc27e684966a01a482ac8481b7561a0b737"),
            tx.classHash,
        )

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)

        when (provider) {
            is GatewayProvider -> {
                receipt = receipt as GatewayTransactionReceipt
                assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
            }
            is JsonRpcProvider -> {
                receipt = receipt as RpcTransactionReceipt
            }
        }
        assertEquals(Felt.fromHex("0x240b93577c4"), receipt.actualFee)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get declare v2 transaction and receipt`(provider: Provider) {
        val transactionHash = Felt.fromHex("0x70fac6862a52000d2d63a1c845c26c9202c9030921b4607818a0820a46eab26")
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV2
        assertNotNull(tx)
        assertNotNull(tx.hash)
        assertNotNull(tx.classHash)
        assertEquals(transactionHash, tx.hash)
        assertEquals(
            Felt.fromHex("0x053bbb3899e1bfb338e9d2687204136c1e5bb89d581bdfdd650689df65b3a836"),
            tx.classHash,
        )
        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        var receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)

        if (provider is GatewayProvider) {
            receipt = receipt as GatewayTransactionReceipt

            assertEquals(TransactionStatus.ACCEPTED_ON_L1, receipt.status)
        } else if (provider is JsonRpcProvider) {
            receipt = receipt as RpcTransactionReceipt
        }

        assertEquals(Felt.fromHex("0x35f91a1984d"), receipt.actualFee)
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `estimate declare v1 transaction fee`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
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
            ExecutionParams(
                nonce = nonce,
                maxFee = Felt(1000000000000000),
            ),
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

        val feeEstimateRequest = provider.getEstimateFee(listOf(signedTransaction.toPayload()), BlockTag.LATEST)

        assertNotNull(feeEstimateRequest)
        val feeEstimate = feeEstimateRequest.send().first()
        assertNotNull(feeEstimate)
        assertNotNull(feeEstimate.gasConsumed)
        assertNotNull(feeEstimate.gasPrice)
        assertNotNull(feeEstimate.overallFee)
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("getAccounts")
    fun `deploy account`(accountAndProvider: AccountAndProvider) {
        val (account, provider) = accountAndProvider
//        val privateKey = Felt(11111)
//        val publicKey = StarknetCurve.getPublicKey(privateKey)

        val classHash = Felt.fromHex("0x37475b8cd1e7360416bae6bc332ba4bc50936cd2d0f9f2207507acbac172e8d")
        val salt = Felt.ONE
//        val calldata = listOf(publicKey)
//        val address = ContractAddressCalculator.calculateAddressFromHash(
//                classHash = classHash,
//                calldata = calldata,
//                salt = salt,
//        )
//        StandardAccountTest.devnetClient.prefundAccount(address)

//        val account = StandardAccount(
//                address,
//                privateKey,
//                provider,
//        )
//        val payload = account.signDeployAccount(
//                classHash = classHash,
//                salt = salt,
//                calldata = calldata,
//                // 10*fee from estimate deploy account fee
//                maxFee = Felt.fromHex("0x11fcc58c7f7000"),
//        )
//
//        val response = provider.deployAccount(payload).send()
//
//        // Make sure the address matches the calculated one
//        assertEquals(address, response.address)
//
//        // Make sure tx matches what we sent
//        val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransaction
//        assertEquals(payload.classHash, tx.classHash)
//        assertEquals(payload.salt, tx.contractAddressSalt)
//        assertEquals(payload.constructorCalldata, tx.constructorCalldata)
//        assertEquals(payload.version, tx.version)
//        assertEquals(payload.nonce, tx.nonce)
//        assertEquals(payload.maxFee, tx.maxFee)
//        assertEquals(payload.signature, tx.signature)

        // Invoke function to make sure the account was deployed properly
        val call = Call(
            contractAddress = accountAddress,
            calldata = listOf(Felt(10)),
            entrypoint = "increase_balance",
        )
        val result = account.execute(call).send()
//        val receipt = provider.getTransactionReceipt(result.transactionHash).send()
//        assertEquals(TransactionStatus.ACCEPTED_ON_L2, receipt.status)
    }

    @ParameterizedTest
    @MethodSource("getProviders")
    fun `get deploy transaction receipt`(provider: Provider) {
        val mockHttpService = spy(OkHttpService())

        doAnswer { invocation ->

            // contruct the real request
//            val payload = invocation.getArgument<HttpService.Payload>(0)
//            val httpRequest = httpService.buildRequest(payload)
//
//            // get the real response
//            val response = httpService.send(httpRequest)
//            // process the HttpResponse and modify the response
//            val originalHttpResponse = httpService.processHttpResponse(response)

            val originalHttpResponse: HttpResponse = invocation.callRealMethod() as HttpResponse

//             Modify the response as you need
//            val originalJson = JsonObject(originalHttpResponse.body)

            val originalJson = Json.parseToJsonElement(originalHttpResponse.body) as JsonObject

//            val modifiedJson = buildJsonObject {
//                putJsonObject("result") {
//                    put("finality_status", JsonPrimitive(TransactionFinalityStatus.ACCEPTED_ON_L1.toString()))
//                    put("execution_status", JsonPrimitive(TransactionExecutionStatus.SUCCEEDED.toString()))
//                }
//            }
//            val status = originalJson["result"]!!.jsonObject["status"]!!.jsonPrimitive.content
            val status = originalJson["result"]!!.jsonObject["status"]?.jsonPrimitive?.content
                ?: TransactionFinalityStatus.ACCEPTED_ON_L1.toString()

            val modifiedJson = JsonObject(
                originalJson["result"]!!.jsonObject.toMutableMap().apply {
                    remove("status")
                    put("finality_status", JsonPrimitive(status))
                    put("execution_status", JsonPrimitive(TransactionExecutionStatus.SUCCEEDED.toString()))
                },
            )

            val mergedJson = JsonObject(
                originalJson.toMutableMap().apply {
                    this["result"] = modifiedJson
                },
            )
            // Construct a new HttpResponse with an adjusted body
            HttpResponse(
                isSuccessful = originalHttpResponse.isSuccessful,
                code = originalHttpResponse.code,
                body = mergedJson.toString(),
            )
        }.`when`(mockHttpService).send(any())

        val currentProvider = when (provider) {
            is JsonRpcProvider -> JsonRpcProvider(rpcUrl, StarknetChainId.TESTNET, mockHttpService)
            else -> provider
        }

        val request = currentProvider.getTransactionReceipt(Felt.fromHex("0x029da9f8997ce580718fa02ed0bd628976418b30a0c5c542510aaef21a4445e4"))
        val response = request.send()

        assertNotNull(response)

        if (currentProvider is GatewayProvider) {
            assertTrue(response is GatewayTransactionReceipt)
        } else if (currentProvider is JsonRpcProvider) {
            assertTrue(response is DeployRpcTransactionReceipt)
        }
    }
}
