package com.swmansion.starknet.data.types

import com.swmansion.starknet.extensions.toFelt

enum class CairoVersion(val version: Felt) {
    ZERO(Felt.ZERO), ONE(Felt.ONE);

    companion object {
        fun fromValue(value: Int): CairoVersion {
            return CairoVersion.entries.firstOrNull { it.version == value.toFelt }
                ?: throw IllegalArgumentException("Unknown Cairo version: $value")
        }

        fun fromValue(value: Felt): CairoVersion {
            return CairoVersion.entries.firstOrNull { it.version == value }
                ?: throw IllegalArgumentException("Unknown Cairo version: $value")
        }
    }
}
