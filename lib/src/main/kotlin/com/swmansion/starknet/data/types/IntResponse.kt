package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.IntResponseSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = IntResponseSerializer::class)
value class IntResponse(val value: Int) : Response
