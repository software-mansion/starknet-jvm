package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.EstimateFeeResponseListSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = EstimateFeeResponseListSerializer::class)
value class EstimateFeeResponseList(val values: List<EstimateFeeResponse>) : Response
