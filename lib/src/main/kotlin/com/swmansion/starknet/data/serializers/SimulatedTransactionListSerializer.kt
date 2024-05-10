package com.swmansion.starknet.data.serializers

import com.swmansion.starknet.data.types.SimulatedTransactionList
import com.swmansion.starknet.data.types.transactions.SimulatedTransaction
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SimulatedTransactionListSerializer : KSerializer<SimulatedTransactionList> {
    private val listSerializer = ListSerializer(SimulatedTransaction.serializer())

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = listSerialDescriptor<SimulatedTransaction>()

    override fun serialize(encoder: Encoder, value: SimulatedTransactionList) {
        encoder.encodeSerializableValue(listSerializer, value.values)
    }

    override fun deserialize(decoder: Decoder): SimulatedTransactionList {
        val list = decoder.decodeSerializableValue(listSerializer)
        return SimulatedTransactionList(list)
    }
}