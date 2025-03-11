package starknet.utils.data.serializers

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import starknet.utils.data.*

internal object SnCastResponsePolymorphicSerializer : JsonContentPolymorphicSerializer<SnCastResponse>(SnCastResponse::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SnCastResponse> {
        val jsonObject = element.jsonObject

        val commandObject = jsonObject.getOrElse("command") { throw IllegalArgumentException("Missing command type in sncast response") }
        val command = Json.decodeFromJsonElement(SnCastCommand.serializer(), commandObject)

        val error = jsonObject["error"]?.jsonPrimitive?.content
        error?.let {
            throw SnCastCommandFailed(commandObject.jsonPrimitive.content, error)
        }

        return when (command) {
            SnCastCommand.ACCOUNT_CREATE -> AccountCreateSnCastResponse.serializer()
            SnCastCommand.ACCOUNT_DEPLOY -> AccountDeploySnCastResponse.serializer()
            SnCastCommand.DECLARE -> DeclareSnCastResponse.serializer()
            SnCastCommand.DEPLOY -> DeploySnCastResponse.serializer()
            SnCastCommand.INVOKE -> InvokeSnCastResponse.serializer()
        }
    }
}
