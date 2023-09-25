package starknet.deployercontract

import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.deployercontract.StandardDeployer
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.gateway.GatewayProvider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import starknet.utils.LegacyDevnetClient
import starknet.utils.MockUtils
import java.nio.file.Path
import java.nio.file.Paths
import starknet.utils.LegacyContractDeployer as TestContractDeployer

@Execution(ExecutionMode.SAME_THREAD)
object StandardDeployerTest {
    @JvmStatic
    private val legacyDevnetClient = LegacyDevnetClient(
        port = 5053,
        accountDirectory = Paths.get("src/test/resources/accounts_legacy/standard_deployer_test"),
    )
    private val signer = StarkCurveSigner(Felt(1234))

    private lateinit var testContractDeployer: TestContractDeployer
    private lateinit var gatewayProvider: Provider
    private lateinit var rpcProvider: Provider
    private lateinit var accountAddress: Felt
    private lateinit var rpcDeployerAddress: Felt
    private lateinit var gatewayDeployerAddress: Felt

    @JvmStatic
    @BeforeAll
    fun before() {
        try {
            legacyDevnetClient.start()

            testContractDeployer = TestContractDeployer.deployInstance(legacyDevnetClient)
            gatewayProvider = GatewayProvider(
                legacyDevnetClient.feederGatewayUrl,
                legacyDevnetClient.gatewayUrl,
                StarknetChainId.TESTNET,
            )

            rpcProvider = JsonRpcProvider(
                legacyDevnetClient.rpcUrl,
                StarknetChainId.TESTNET,
            )

            val (classHash, _) = legacyDevnetClient.declareContract(Path.of("src/test/resources/compiled_v0/deployer.json"))
            rpcDeployerAddress = testContractDeployer.deployContract(classHash)
            gatewayDeployerAddress = testContractDeployer.deployContract(classHash)

            deployAccount()
        } catch (ex: Exception) {
            legacyDevnetClient.close()
            throw ex
        }
    }

    private fun deployAccount() {
        val (classHash, _) = legacyDevnetClient.declareContract(Path.of("src/test/resources/compiled_v0/account.json"))
        accountAddress = testContractDeployer.deployContract(classHash, calldata = listOf(signer.publicKey))
        legacyDevnetClient.prefundAccount(accountAddress)
    }

    data class StandardDeployerAndProvider(val standardDeployer: StandardDeployer, val provider: Provider)

    @JvmStatic
    fun getStandardDeployerAndProvider(): List<StandardDeployerAndProvider> {
        return listOf(
            StandardDeployerAndProvider(
                StandardDeployer(
                    gatewayDeployerAddress,
                    gatewayProvider,
                    StandardAccount(accountAddress, signer, gatewayProvider),
                ),
                gatewayProvider,
            ),
            StandardDeployerAndProvider(
                StandardDeployer(
                    rpcDeployerAddress,
                    rpcProvider,
                    StandardAccount(accountAddress, signer, rpcProvider),
                ),
                rpcProvider,
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerAndProvider")
    fun `test udc deploy`(standardDeployerAndProvider: StandardDeployerAndProvider) {
        val (standardDeployer, provider) = standardDeployerAndProvider
        val (receiptStandartDeployer, receiptProvider) = when (provider) {
            is GatewayProvider -> Pair(standardDeployer, provider)
            is JsonRpcProvider -> {
                val mockProvider = MockUtils.mockUpdatedReceiptRpcProvider(provider)
                Pair(
                    StandardDeployer(rpcDeployerAddress, mockProvider, StandardAccount(accountAddress, signer, rpcProvider)),
                    mockProvider,
                )
            }
            else -> throw IllegalStateException("Unknown provider type")
        }

        val (classHash, _) = legacyDevnetClient.declareContract(Path.of("src/test/resources/compiled_v0/balance.json"))
        val deployment = standardDeployer.deployContract(classHash, true, Felt(1234), emptyList()).send()
        val address = receiptStandartDeployer.findContractAddress(deployment).send()

        assertNotNull(address)
        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerAndProvider")
    fun `test udc deploy with default parameters`(standardDeployerAndProvider: StandardDeployerAndProvider) {
        val (standardDeployer, provider) = standardDeployerAndProvider
        val (receiptStandartDeployer, receiptProvider) = when (provider) {
            is GatewayProvider -> Pair(standardDeployer, provider)
            is JsonRpcProvider -> {
                val mockProvider = MockUtils.mockUpdatedReceiptRpcProvider(provider)
                Pair(
                    StandardDeployer(rpcDeployerAddress, mockProvider, StandardAccount(accountAddress, signer, rpcProvider)),
                    mockProvider,
                )
            }
            else -> throw IllegalStateException("Unknown provider type")
        }

        val (classHash, _) = legacyDevnetClient.declareContract(Path.of("src/test/resources/compiled_v0/balance.json"))
        val deployment = standardDeployer.deployContract(classHash, emptyList()).send()
        val address = receiptStandartDeployer.findContractAddress(deployment).send()

        assertNotNull(address)
        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
    }

    @ParameterizedTest
    @MethodSource("getStandardDeployerAndProvider")
    fun `test udc deploy with constructor`(standardDeployerAndProvider: StandardDeployerAndProvider) {
        val (standardDeployer, provider) = standardDeployerAndProvider
        val (receiptStandartDeployer, receiptProvider) = when (provider) {
            is GatewayProvider -> Pair(standardDeployer, provider)
            is JsonRpcProvider -> {
                val mockProvider = MockUtils.mockUpdatedReceiptRpcProvider(provider)
                Pair(
                    StandardDeployer(rpcDeployerAddress, mockProvider, StandardAccount(accountAddress, signer, rpcProvider)),
                    mockProvider,
                )
            }
            else -> throw IllegalStateException("Unknown provider type")
        }

        val constructorValue = Felt(111)
        val (classHash, _) = legacyDevnetClient.declareContract(Path.of("src/test/resources/compiled_v0/contractWithConstructor.json"))
        val deployment =
            standardDeployer.deployContract(classHash, true, Felt(1234), listOf(constructorValue, Felt(789))).send()
        val address = receiptStandartDeployer.findContractAddress(deployment).send()

        val contractValue = provider.callContract(Call(address, "get_val1"), BlockTag.LATEST).send()

        assertNotNull(address)
        assertEquals(listOf(constructorValue), contractValue)
    }

//    This test will fail on systemc other than linux x64, because of sierra compiler
//    @ParameterizedTest
//    @MethodSource("getStandardDeployerAndProvider")
//    fun `test udc deploy with cairo1 contract`(standardDeployerAndProvider: StandardDeployerAndProvider) {
//        val (standardDeployer, provider) = standardDeployerAndProvider
//        val (classHash, _) = devnetClient.declareV2Contract(Path.of("src/test/resources/compiled_v1/${provider::class.simpleName}_hello_starknet.json"))
//        val deployment = standardDeployer.deployContract(classHash, emptyList()).send()
//        val address = standardDeployer.findContractAddress(deployment).send()
//
//        assertNotNull(address)
//        assertDoesNotThrow { provider.callContract(Call(address, "get_balance"), BlockTag.LATEST).send() }
//    }
}
