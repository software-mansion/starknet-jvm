package network.utils

import com.swmansion.starknet.data.types.Felt

class NetworkConfig {
    data class Config(
        val network: Network,
        val rpcUrl: String,
        val accountAddress: Felt,
        val privateKey: Felt,
        val cairoVersion: Int = 0,
        val constNonceAccountAddress: Felt? = null,
        val constNoncePrivateKey: Felt? = null,
    )

    enum class Network(val value: String) {
        SEPOLIA_INTEGRATION("SEPOLIA_INTEGRATION"),
        SEPOLIA_TESTNET("SEPOLIA_TESTNET"),
    }

    enum class NetworkTestMode {
        DISABLED,
        NON_GAS,
        ALL,
        ;

        companion object {
            @JvmStatic
            fun fromString(value: String): NetworkTestMode {
                return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DISABLED
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
                    network = Network.SEPOLIA_INTEGRATION,
                    rpcUrl = "",
                    accountAddress = Felt.ZERO,
                    privateKey = Felt.ZERO,
                )
            }

            val env = System.getenv()
            val network = Network.valueOf(
                env.getOrElse("NETWORK_TEST_NETWORK_NAME") { throw RuntimeException("NETWORK_TEST_NETWORK_NAME not found in environment variables") },
            )
            return Config(
                network = network,
                rpcUrl = "${network.value}_RPC_URL".let { env.getOrElse(it) { throw RuntimeException("$it not found in environment variables") } },
                accountAddress = "${network.value}_ACCOUNT_ADDRESS".let { env.getOrElse(it) { throw RuntimeException("$it not found in environment variables") } }.let { Felt.fromHex(it) },
                privateKey = "${network.value}_PRIVATE_KEY".let { env.getOrElse(it) { throw RuntimeException("$it not found in environment variables") } }.let { Felt.fromHex(it) },
                cairoVersion = env["${network.value}_ACCOUNT_CAIRO_VERSION"]?.toInt() ?: 0,
                constNonceAccountAddress = env["${network.value}_CONST_NONCE_ACCOUNT_ADDRESS"]?.let { Felt.fromHex(it) },
                constNoncePrivateKey = env["${network.value}_CONST_NONCE_PRIVATE_KEY"]?.let { Felt.fromHex(it) },
            )
        }

        val config: Config by lazy {
            makeConfigFromEnv()
        }
    }
}
