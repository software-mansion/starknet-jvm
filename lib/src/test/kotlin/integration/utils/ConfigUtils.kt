package integration.utils

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.Serializable

class ConfigUtils {
    @Serializable
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
        fun isTestEnabled(requiresGas: Boolean): Boolean {
            val integrationTestsEnabled = System.getProperty("enableIntegrationTests")?.toBoolean() ?: true
            if (!integrationTestsEnabled) {
                return false
            }
            if (requiresGas) {
                return System.getProperty("enableGasTests")?.toBoolean() ?: false
            }

            return true
        }

        private fun makeConfigFromEnv(): Config {
            return Config(
                rpcUrl = System.getenv("INTEGRATION_RPC_URL")
                    ?: throw RuntimeException("INTEGRATION_RPC_URL not found in environment variables"),
                gatewayUrl = System.getenv("INTEGRATION_GATEWAY_URL") ?: Config.DEFAULT_GATEWAY_URL,
                feederGatewayUrl = System.getenv("STARKNET_JVM_INTEGRATION_FEEDER_GATEWAY_URL") ?: Config.DEFAULT_FEEDER_GATEWAY_URL,
                accountAddress = Felt.fromHex(
                    System.getenv("INTEGRATION_ACCOUNT_ADDRESS")
                        ?: throw RuntimeException("INTEGRATION_ACCOUNT_ADDRESS not found in environment variables"),
                ),
                privateKey = Felt.fromHex(
                    System.getenv("INTEGRATION_PRIVATE_KEY")
                        ?: throw RuntimeException("INTEGRATION_PRIVATE_KEY not found in environment variables"),
                ),
            )
        }

        val config: Config
            get() = makeConfigFromEnv()
    }
}
