package starknet.utils.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class ScarbBuildStatus {
    @JsonNames("compiling")
    COMPILING,

    @JsonNames("finished")
    FINISHED,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
enum class ScarbBuildType {
    @JsonNames("error")
    ERROR,
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ScarbBuildResponse(
    @JsonNames("status")
    val status: ScarbBuildStatus? = null,

    @JsonNames("type")
    val type: ScarbBuildType? = null,

    @JsonNames("message")
    val message: String? = null,
)

class ScarbCommandFailed(val commandName: String, val error: String?) :
    Exception("Command $commandName failed. Error reason: $error")
