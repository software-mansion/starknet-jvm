package starknet.utils.data.serializers

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import starknet.utils.data.AccountDetails
import starknet.utils.data.DevnetSetupFailedException

class AccountDetailsSerializer(val name: String) :
    JsonTransformingSerializer<AccountDetails>(AccountDetails.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val accounts = element.jsonObject.getOrElse("alpha-sepolia") { throw DevnetSetupFailedException("Invalid account file") }
        return accounts.jsonObject.getOrElse(name) { throw DevnetSetupFailedException("Details for account \"$name\" not found") }
    }
}
