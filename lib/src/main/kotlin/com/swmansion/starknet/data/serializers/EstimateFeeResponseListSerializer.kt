package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.EstimateFeeResponse
import com.swmansion.starknet.data.types.EstimateFeeResponseList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object EstimateFeeResponseListSerializer : KSerializer<EstimateFeeResponseList> {
    private val listSerializer = ListSerializer(EstimateFeeResponse.serializer())

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = listSerialDescriptor<EstimateFeeResponse>()

    override fun serialize(encoder: Encoder, value: EstimateFeeResponseList) {
        encoder.encodeSerializableValue(listSerializer, value.values)
    }

    override fun deserialize(decoder: Decoder): EstimateFeeResponseList {
        val list = decoder.decodeSerializableValue(listSerializer)
        return EstimateFeeResponseList(list)
    }
}
