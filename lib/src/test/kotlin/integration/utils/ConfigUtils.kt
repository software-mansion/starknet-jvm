package integration.utils

import com.swmansion.starknet.data.types.Felt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.readText

class ConfigUtils {
    @Serializable
    data class Config(
        @JsonNames("rpc_url")
        val rpcUrl: String,

        @JsonNames("gateway_url")
        val gatewayUrl: String = DEFAULT_GATEWAY_URL,

        @JsonNames("feeder_gateway_url")
        val feederGatewayUrl: String = DEFAULT_FEEDER_GATEWAY_URL,

        @JsonNames("integration_account_address")
        val integrationAccountAddress: Felt,

        @JsonNames("private_key")
        val privateKey: Felt,
    ) {
        companion object {
            const val DEFAULT_GATEWAY_URL = "https://external.integration.starknet.io/gateway"
            const val DEFAULT_FEEDER_GATEWAY_URL = "https://external.integration.starknet.io/feeder_gateway"
        }
    }
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun isTestSetSkipped(name: String): Boolean {
            val screamingSnakeName = name.replace(Regex("([a-z])([A-Z])"), "$1_$2")?.uppercase()
            val envVar = "STARKNET_JVM_SKIP_INTEGRATION_$screamingSnakeName"

            return System.getenv(envVar)?.lowercase()?.toBoolean() ?: false
        }
        private fun makeConfigFromEnv(): Config {
            return Config(
                rpcUrl = System.getenv("STARKNET_JVM_RPC_URL")
                    ?: throw RuntimeException("RPC_URL not found in environment variables"),
                gatewayUrl = System.getenv("STARKNET_JVM_GATEWAY_URL") ?: Config.DEFAULT_GATEWAY_URL,
                feederGatewayUrl = System.getenv("STARKNET_JVM_FEEDER_GATEWAY_URL") ?: Config.DEFAULT_FEEDER_GATEWAY_URL,
                integrationAccountAddress = Felt.fromHex(
                    System.getenv("STARKNET_JVM_INTEGRATION_ACCOUNT_ADDRESS")
                        ?: throw RuntimeException("INTEGRATION_ACCOUNT_ADDRESS not found in environment variables"),
                ),
                privateKey = Felt.fromHex(
                    System.getenv("STARKNET_JVM_INTEGRATION_PRIVATE_KEY")
                        ?: throw RuntimeException("INTEGRATION_PRIVATE_KEY not found in environment variables"),
                ),
            )
        }
        private fun makeConfigFromFile(path: String): Config {
            val configPath = Paths.get(path)
            if (!Files.exists(configPath)) {
                throw IllegalStateException("Config file not found")
            }

            return json.decodeFromString(Config.serializer(), configPath.readText())
        }

        val config: Config
            get() {
                val configSource = System.getenv("STARKNET_JVM_INTEGRATION_TEST_SOURCE")?.lowercase()
                    ?: "file"

                return when (configSource) {
                    "env" -> makeConfigFromEnv()
                    "file" -> makeConfigFromFile("src/test/kotlin/integration/config.json")
                    else -> throw IllegalStateException("Invalid configuration source provided")
                }
            }
    }
}
