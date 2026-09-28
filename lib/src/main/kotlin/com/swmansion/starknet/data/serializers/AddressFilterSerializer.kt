package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.AddressFilter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.encodeToJsonElement

internal object AddressFilterSerializer : KSerializer<AddressFilter> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AddressFilter", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AddressFilter) {
        require(encoder is JsonEncoder)
        val element = when (value) {
            is AddressFilter.Single -> encoder.json.encodeToJsonElement(FeltSerializer, value.address)
            is AddressFilter.Multiple -> encoder.json.encodeToJsonElement(ListSerializer(FeltSerializer), value.addresses)
        }
        encoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): AddressFilter {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return if (element is JsonArray) {
            val addresses = element.map { decoder.json.decodeFromJsonElement(FeltSerializer, it) }
            AddressFilter.Multiple(addresses)
        } else {
            val address = decoder.json.decodeFromJsonElement(FeltSerializer, element)
            AddressFilter.Single(address)
        }
    }
}
