package network.utils

import com.swmansion.starknet.data.types.Felt

class NetworkConfig {
    data class Config(
        val rpcUrl: String,
        val gatewayUrl: String = DEFAULT_GATEWAY_URL,
        val feederGatewayUrl: String = DEFAULT_FEEDER_GATEWAY_URL,
        val accountAddress: Felt,
        val privateKey: Felt,
        val constNonceAccountAddress: Felt? = null,
        val constNoncePrivateKey: Felt? = null,
    ) {
        companion object {
            const val DEFAULT_GATEWAY_URL = "https://external.integration.starknet.io/gateway"
            const val DEFAULT_FEEDER_GATEWAY_URL = "https://external.integration.starknet.io/feeder_gateway"
        }
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
                    rpcUrl = "",
                    gatewayUrl = "",
                    feederGatewayUrl = "",
                    accountAddress = Felt.ZERO,
                    privateKey = Felt.ZERO,
                )
            }

            val env = System.getenv()
            return Config(
                rpcUrl = env.getOrElse("STARKNET_RPC_URL") { throw RuntimeException("STARKNET_RPC_URL not found in environment variables") },
                gatewayUrl = env.getOrDefault("STARKNET_GATEWAY_URL", Config.DEFAULT_GATEWAY_URL),
                feederGatewayUrl = env.getOrDefault("STARKNET_JVM_STARKNET_FEEDER_GATEWAY_URL", Config.DEFAULT_FEEDER_GATEWAY_URL),
                accountAddress = Felt.fromHex(
                    env.getOrElse("STARKNET_ACCOUNT_ADDRESS") { throw RuntimeException("STARKNET_ACCOUNT_ADDRESS not found in environment variables") },
                ),
                privateKey = Felt.fromHex(
                    env.getOrElse("STARKNET_PRIVATE_KEY") { throw RuntimeException("STARKNET_PRIVATE_KEY not found in environment variables") },
                ),
                constNonceAccountAddress = env["STARKNET_CONST_NONCE_ACCOUNT_ADDRESS"]?.let { Felt.fromHex(it) },
                constNoncePrivateKey = env["STARKNET_CONST_NONCE_PRIVATE_KEY"]?.let { Felt.fromHex(it) },
            )
        }

        val config: Config by lazy {
            makeConfigFromEnv()
        }
    }
}
