package com.swmansion.starknet.data.types

import com.swmansion.starknet.extensions.toFelt

/**
 * The version of Cairo language in which contract is written.
 */
enum class CairoVersion(val version: Felt) {
    /**
     * Corresponds to the legacy Version of Cairo.
     */
    ZERO(Felt.ZERO),

    /**
     * Corresponds to any contract compiled with Cairo >= 1.
     */
    ONE(Felt.ONE);

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
