package com.swmansion.starknet.crypto

import com.swmansion.starknet.data.types.Felt

enum class HashMethod {
    PEDERSEN {
        override fun hash(first: Felt, second: Felt): Felt {
            return StarknetCurve.pedersen(first, second)
        }
    },
    POSEIDON {
        override fun hash(first: Felt, second: Felt): Felt {
            return Poseidon.poseidonHash(first, second)
        }
    }, ;

    abstract fun hash(first: Felt, second: Felt): Felt
}
