package integration.utils

import com.swmansion.starknet.data.types.Felt

class IntegrationConfig {
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

    enum class IntegrationTestMode {
        DISABLED,
        NON_GAS,
        ALL,
        ;

        companion object {
            @JvmStatic
            fun fromString(value: String): IntegrationTestMode {
                return values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DISABLED
            }
        }
    }

    companion object {
        private val integrationTestMode: IntegrationTestMode =
            IntegrationTestMode.fromString(System.getProperty("integrationTestMode") ?: "disabled")

        @JvmStatic
        fun isTestEnabled(requiresGas: Boolean): Boolean {
            return when (integrationTestMode) {
                IntegrationTestMode.DISABLED -> false
                IntegrationTestMode.NON_GAS -> !requiresGas
                IntegrationTestMode.ALL -> true
            }
        }

        @JvmStatic
        private fun makeConfigFromEnv(): Config {
            if (integrationTestMode == IntegrationTestMode.DISABLED) {
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
                rpcUrl = env.getOrElse("INTEGRATION_RPC_URL") { throw RuntimeException("INTEGRATION_RPC_URL not found in environment variables") },
                gatewayUrl = env.getOrDefault("INTEGRATION_GATEWAY_URL", Config.DEFAULT_GATEWAY_URL),
                feederGatewayUrl = env.getOrDefault("STARKNET_JVM_INTEGRATION_FEEDER_GATEWAY_URL", Config.DEFAULT_FEEDER_GATEWAY_URL),
                accountAddress = Felt.fromHex(
                    env.getOrElse("INTEGRATION_ACCOUNT_ADDRESS") { throw RuntimeException("INTEGRATION_ACCOUNT_ADDRESS not found in environment variables") },
                ),
                privateKey = Felt.fromHex(
                    env.getOrElse("INTEGRATION_PRIVATE_KEY") { throw RuntimeException("INTEGRATION_PRIVATE_KEY not found in environment variables") },
                ),
                constNonceAccountAddress = env["INTEGRATION_CONST_NONCE_ACCOUNT_ADDRESS"]?.let { Felt.fromHex(it) },
                constNoncePrivateKey = env["INTEGRATION_CONST_NONCE_PRIVATE_KEY"]?.let { Felt.fromHex(it) },
            )
        }

        val config: Config by lazy {
            makeConfigFromEnv()
        }
    }
}
