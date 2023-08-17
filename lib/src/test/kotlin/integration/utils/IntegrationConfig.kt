package integration.utils

import com.swmansion.starknet.data.types.Felt

class IntegrationConfig {
    data class Config(
        val rpcUrl: String,
        val gatewayUrl: String = DEFAULT_GATEWAY_URL,
        val feederGatewayUrl: String = DEFAULT_FEEDER_GATEWAY_URL,
        val accountAddress: Felt,
        val privateKey: Felt,
    ) {
        companion object {
            const val DEFAULT_GATEWAY_URL = "https://external.integration.starknet.io/gateway"
            const val DEFAULT_FEEDER_GATEWAY_URL = "https://external.integration.starknet.io/feeder_gateway"
        }
    }
    companion object {
        @JvmStatic
        fun isTestEnabled(requiresGas: Boolean): Boolean {
            val enableIntegrationTests = System.getProperty("enableIntegrationTests")?.toBoolean() ?: false
            val enableGasTests = System.getProperty("enableGasTests")?.toBoolean() ?: false

            if (!enableIntegrationTests) {
                return false
            }
            if (requiresGas) {
                return enableGasTests
            }

            return true
        }

        @JvmStatic
        private fun makeConfigFromEnv(): Config {
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
            )
        }

        val config: Config by lazy {
            makeConfigFromEnv()
        }
    }
}
