package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.MessageStatus
import com.swmansion.starknet.data.types.MessageStatusList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object MessageStatusListSerializer : KSerializer<MessageStatusList> {
    private val listSerializer = ListSerializer(MessageStatus.serializer())

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = listSerialDescriptor<MessageStatus>()

    override fun serialize(encoder: Encoder, value: MessageStatusList) {
        encoder.encodeSerializableValue(listSerializer, value.values)
    }

    override fun deserialize(decoder: Decoder): MessageStatusList {
        val list = decoder.decodeSerializableValue(listSerializer)
        return MessageStatusList(list)
    }
}
