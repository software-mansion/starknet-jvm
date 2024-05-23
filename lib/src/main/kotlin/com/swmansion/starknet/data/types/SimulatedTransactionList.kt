package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.SimulatedTransactionListSerializer
import kotlinx.serialization.Serializable

@JvmInline
@Serializable(with = SimulatedTransactionListSerializer::class)
value class SimulatedTransactionList(val values: List<SimulatedTransaction>) : StarknetResponse
