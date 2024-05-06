package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class FeltArray(private val list: MutableList<Felt>) : ConvertibleToCalldata, MutableList<Felt> by list, HttpBatchRequestType {
    constructor(vararg elements: Felt) : this(elements.toMutableList())
    constructor(collection: Collection<Felt>) : this(collection.toMutableList())
    constructor() : this(emptyList())

    override fun toCalldata(): List<Felt> = list.toList()
}

object FeltArraySerializer : KSerializer<FeltArray> {
    private val delegate = ListSerializer(Felt.serializer())

    override val descriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: FeltArray) {
        delegate.serialize(encoder, value.toList())
    }

    override fun deserialize(decoder: Decoder): FeltArray {
        return FeltArray(delegate.deserialize(decoder))
    }
}
