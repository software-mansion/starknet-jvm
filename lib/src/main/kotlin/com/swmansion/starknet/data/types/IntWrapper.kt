package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.IntWrapperSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = IntWrapperSerializer::class)
value class IntWrapper(val value: Int) : HttpBatchRequestType
