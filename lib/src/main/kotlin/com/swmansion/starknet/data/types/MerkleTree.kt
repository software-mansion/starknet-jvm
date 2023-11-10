package com.swmansion.starknet.data.types

import com.swmansion.starknet.crypto.StarknetCurve

data class MerkleTree(
    val leafHashes: List<Felt>,
) {
    private val mutableBranches: MutableList<MutableList<Felt>> = mutableListOf()
    val root: Felt = build(leafHashes, mutableBranches)
    val branches: List<List<Felt>> = mutableBranches.toList()

    private fun build(
        leaves: List<Felt>,
        branches: MutableList<MutableList<Felt>>,
    ): Felt {
        if (leaves.size == 1) {
            return leaves[0]
        }
        if (leaves.size != leafHashes.size) {
            branches.add(leaves.toMutableList())
        }
        val newLeaves = mutableListOf<Felt>()
        for (i in leaves.indices step 2) {
            if (i + 1 == leaves.size) {
                newLeaves.add(hash(leaves[i], Felt.ZERO))
            } else {
                newLeaves.add(hash(leaves[i], leaves[i + 1]))
            }
        }
        return build(newLeaves, branches)
    }

    private fun hash(a: Felt, b: Felt): Felt {
        val (aSorted, bSorted) = listOf(a, b).sorted()
        return StarknetCurve.pedersen(aSorted, bSorted)
    }
}
