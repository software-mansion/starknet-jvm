package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.InlineStringWrapperSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = InlineStringWrapperSerializer::class)
value class InlineStringWrapper(val value: String) : HttpBatchRequestType
