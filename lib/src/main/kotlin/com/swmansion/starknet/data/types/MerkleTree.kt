package com.swmansion.starknet.data.types

import com.swmansion.starknet.crypto.StarknetCurve

data class MerkleTree(
    val leafHashes: List<Felt>,
) {
    private val buildResult = build(leafHashes)

    val rootHash: Felt = buildResult.first
    val branches: List<List<Felt>> = buildResult.second

    companion object {
        @JvmStatic
        internal fun hash(a: Felt, b: Felt): Felt {
            val (aSorted, bSorted) = if (a < b) Pair(a, b) else Pair(b, a)
            return StarknetCurve.pedersen(aSorted, bSorted)
        }
    }

    private fun build(leaves: List<Felt>): Pair<Felt, List<List<Felt>>> {
        require(leaves.isNotEmpty()) { "Cannot build Merkle tree from an empty list of leaves." }
        return build(leaves, emptyList())
    }

    private fun build(leaves: List<Felt>, branches: List<List<Felt>>): Pair<Felt, List<List<Felt>>> {
        if (leaves.size == 1) {
            return leaves[0] to branches
        }
        val newBranches = when (leaves.size != leafHashes.size) {
            true -> branches + listOf(leaves)
            false -> branches
        }
        val newLeaves = leaves.indices.step(2).map {
            hash(leaves[it], leaves.getOrElse(it + 1) { Felt.ZERO })
        }

        return build(newLeaves, newBranches)
    }
}
