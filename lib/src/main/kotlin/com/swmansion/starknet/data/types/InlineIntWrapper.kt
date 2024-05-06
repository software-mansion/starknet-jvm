package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.InlineIntWrapperSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = InlineIntWrapperSerializer::class)
value class InlineIntWrapper(val value: Int) : HttpBatchRequestType
