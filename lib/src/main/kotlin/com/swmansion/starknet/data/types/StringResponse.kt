package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.StringResponseSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = StringResponseSerializer::class)
value class StringResponse(val value: String) : Response
