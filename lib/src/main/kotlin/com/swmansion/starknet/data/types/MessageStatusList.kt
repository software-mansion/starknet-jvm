package com.swmansion.starknet.data.types

import com.swmansion.starknet.data.serializers.MessageStatusListSerializer
import kotlinx.serialization.Serializable

@Serializable(with = MessageStatusListSerializer::class)
data class MessageStatusList(val values: List<MessageStatus>) : StarknetResponse
