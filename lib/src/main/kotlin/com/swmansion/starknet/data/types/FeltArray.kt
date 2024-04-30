package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata

data class FeltArray(private val list: MutableList<Felt>) : ConvertibleToCalldata, MutableList<Felt> by list {
    constructor(vararg elements: Felt) : this(elements.toMutableList())
    constructor(collection: Collection<Felt>) : this(collection.toMutableList())
    constructor() : this(emptyList())

    override fun toCalldata(): List<Felt> {
        val arraySize = Felt(list.size.toBigInteger())
        return listOf(arraySize) + list
    }
}
