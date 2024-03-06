package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt

enum class HashMethod {
    PEDERSEN {
        override fun hash(first: Felt, second: Felt): Felt {
            return StarknetCurve.pedersen(first, second)
        }
        override fun hash(values: List<Felt>): Felt {
            return StarknetCurve.pedersenOnElements(values)
        }
    },
    POSEIDON {
        override fun hash(first: Felt, second: Felt): Felt {
            return Poseidon.poseidonHash(first, second)
        }
        override fun hash(values: List<Felt>): Felt {
            return Poseidon.poseidonHash(values)
        }
    }, ;

    abstract fun hash(first: Felt, second: Felt): Felt
    abstract fun hash(values: List<Felt>): Felt
}
