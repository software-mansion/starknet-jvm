package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.StarknetStringSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = StarknetStringSerializer::class)
value class StarknetString(val value: String) : HttpBatchRequestType
