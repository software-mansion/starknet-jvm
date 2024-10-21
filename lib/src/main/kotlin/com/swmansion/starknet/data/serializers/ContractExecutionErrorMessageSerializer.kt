package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.provider.rpc.ContractExecutionError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object ContractExecutionErrorMessageSerializer : KSerializer<ContractExecutionError.ErrorMessage> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContractExecutionError.ErrorMessage", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ContractExecutionError.ErrorMessage {
        val stringValue = decoder.decodeString()
        return ContractExecutionError.ErrorMessage(stringValue)
    }

    override fun serialize(encoder: Encoder, value: ContractExecutionError.ErrorMessage) {
        encoder.encodeString(value.value)
    }
}