package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.types.conversions.ConvertibleToCalldata
import com.swmansion.starknet.extensions.toFelt

data class FeltArray(private val list: MutableList<Felt>) : ConvertibleToCalldata, MutableList<Felt> by list {
    constructor(vararg elements: Felt) : this(elements.toMutableList())

    @JvmOverloads
    constructor(collection: Collection<Felt> = emptyList()) : this(collection.toMutableList())

    override fun toCalldata(): List<Felt> = listOf(size.toFelt) + list
}
