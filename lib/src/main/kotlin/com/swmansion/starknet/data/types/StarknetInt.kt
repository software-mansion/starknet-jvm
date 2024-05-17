package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.StarknetIntSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = StarknetIntSerializer::class)
value class StarknetInt(val value: Int) : HttpRequestType
