package starknet.utils

import kotlinx.serialization.json.*
import starknet.utils.data.*
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ScarbClient {
    companion object {
        @JvmStatic
        private val json = Json { ignoreUnknownKeys = true }

        @JvmStatic
        @JvmOverloads
        fun buildContracts(contractsDirectory: Path, profileName: String = "release"): ScarbBuildResponse {
            val output = runScarb(
                command = "build",
                args = emptyList(),
                workingDirectory = contractsDirectory,
                profileName = profileName,
            )
            // `scarb build` outputs multiple json strings. Only parse the last one.
            val index = output.lastIndexOf('{')
            val resultJson = if (index >= 0) output.substring(index) else throw IllegalArgumentException("Invalid response JSON")

            val result = json.decodeFromString(ScarbBuildResponse.serializer(), resultJson)
            if (result.status != ScarbBuildStatus.FINISHED || result.type == ScarbBuildType.ERROR) {
                throw ScarbCommandFailed("build", result.message)
            }

            return result
        }

        @JvmStatic
        private fun runScarb(
            command: String,
            args: List<String>,
            workingDirectory: Path,
            profileName: String,
        ): String {
            val processBuilder = ProcessBuilder(
                "scarb",
                "--profile",
                profileName,
                "--json",
                command,
                *(args.toTypedArray()),
            )
            processBuilder.directory(File(workingDirectory.absolutePathString()))

            val process = processBuilder.start()
            process.waitFor()

            val error = String(process.errorStream.readAllBytes())
            requireNoErrors(command, error)

            val output = String(process.inputStream.readAllBytes())
            return output
        }

        @JvmStatic
        @Synchronized
        fun buildSaltedContract(
            placeholderContractPath: Path,
            saltedContractPath: Path,
            placeholderText: String = "__placeholder__",
            saltText: String = "t${System.currentTimeMillis()}",
        ) {
            val originalCode = saltedContractPath.readText()

            createSaltedContract(
                placeholderContractPath = placeholderContractPath,
                saltedContractPath = saltedContractPath,
                placeholderText = placeholderText,
                saltText = saltText,
            )
            buildContracts(saltedContractPath.parent)

            saltedContractPath.writeText(originalCode)
        }

        @JvmStatic
        private fun createSaltedContract(placeholderContractPath: Path, saltedContractPath: Path, placeholderText: String, saltText: String) {
            val contractCode = placeholderContractPath.readText()
            val newContractCode = contractCode.replace(placeholderText, saltText)

            saltedContractPath.writeText(newContractCode)
        }

        private fun requireNoErrors(command: String, errorStream: String) {
            if (errorStream.isNotEmpty()) {
                throw ScarbCommandFailed(command, errorStream)
            }
        }
    }
}
