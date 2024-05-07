package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.StringWrapperSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = StringWrapperSerializer::class)
value class StringWrapper(val value: String) : HttpBatchRequestType
