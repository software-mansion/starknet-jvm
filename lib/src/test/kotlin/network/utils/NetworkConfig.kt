package network.utils

import com.swmansion.starknet.data.NetUrls
import com.swmansion.starknet.data.types.Felt

class NetworkConfig {
    data class Config(
        val network: Network,
        val rpcUrl: String,
        val gatewayUrl: String,
        val feederGatewayUrl: String,
        val accountAddress: Felt,
        val privateKey: Felt,
        val constNonceAccountAddress: Felt? = null,
        val constNoncePrivateKey: Felt? = null,
    ) {
        companion object {

            @JvmStatic
            fun getDefaultGatewayURL(network: Network): String {
                val baseUrl = when (network) {
                    Network.INTEGRATION -> NetUrls.INTEGRATION_URL
                    Network.TESTNET -> NetUrls.TESTNET_URL
                }
                return "$baseUrl/gateway"
            }

            @JvmStatic
            fun getDefaultFeederGatewayURL(network: Network): String {
                val baseUrl = when (network) {
                    Network.INTEGRATION -> NetUrls.INTEGRATION_URL
                    Network.TESTNET -> NetUrls.TESTNET_URL
                }
                return "$baseUrl/feeder_gateway"
            }
        }
    }

    enum class Network(val value: String) {
        INTEGRATION("INTEGRATION"),
        TESTNET("TESTNET"),
    }

    enum class NetworkTestMode {
        DISABLED,
        NON_GAS,
        ALL;

        companion object {
            @JvmStatic
            fun fromString(value: String): NetworkTestMode {
                return values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DISABLED
            }
        }
    }

    companion object {
        private val testMode: NetworkTestMode =
            NetworkTestMode.fromString(System.getProperty("networkTestMode") ?: "disabled")

        @JvmStatic
        fun isTestEnabled(requiresGas: Boolean): Boolean {
            return when (testMode) {
                NetworkTestMode.DISABLED -> false
                NetworkTestMode.NON_GAS -> !requiresGas
                NetworkTestMode.ALL -> true
            }
        }

        @JvmStatic
        private fun makeConfigFromEnv(): Config {
            if (testMode == NetworkTestMode.DISABLED) {
                return Config(
                    network = Network.INTEGRATION,
                    rpcUrl = "",
                    gatewayUrl = "",
                    feederGatewayUrl = "",
                    accountAddress = Felt.ZERO,
                    privateKey = Felt.ZERO,
                )
            }

            val env = System.getenv()
            val network = Network.valueOf(
                env.getOrElse("NETWORK_TEST_NETWORK") { throw RuntimeException("NETWORK_TEST_NETWORK not found in environment variables") },
            )
            return Config(
                network = network,
                rpcUrl = env.getOrElse("${network.value}_RPC_URL") { throw RuntimeException("${network.value}_RPC_URL not found in environment variables") },
                gatewayUrl = env.getOrDefault("${network.value}_GATEWAY_URL", Config.getDefaultGatewayURL(network)),
                feederGatewayUrl = env.getOrDefault("${network.value}_FEEDER_GATEWAY_URL", Config.getDefaultFeederGatewayURL(network)),
                accountAddress = Felt.fromHex(
                    env.getOrElse("${network.value}_ACCOUNT_ADDRESS") { throw RuntimeException("${network.value}_ACCOUNT_ADDRESS not found in environment variables") },
                ),
                privateKey = Felt.fromHex(
                    env.getOrElse("${network.value}_PRIVATE_KEY") { throw RuntimeException("${network.value}_PRIVATE_KEY not found in environment variables") },
                ),
                constNonceAccountAddress = env["${network.value}_CONST_NONCE_ACCOUNT_ADDRESS"]?.let { Felt.fromHex(it) },
                constNoncePrivateKey = env["${network.value}_CONST_NONCE_PRIVATE_KEY"]?.let { Felt.fromHex(it) },
            )
        }

        val config: Config by lazy {
            makeConfigFromEnv()
        }
    }
}
