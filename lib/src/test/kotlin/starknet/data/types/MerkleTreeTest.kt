package starknet.data.types

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MerkleTreeTest {
    @Test
    fun `calculate hashes`() {
        val leaves = listOf(
            Felt.fromHex("0x12"),
            Felt.fromHex("0xa"),
        )
        val merkleHash = MerkleTree.hash(leaves[0], leaves[1])
        val rawHash = StarknetCurve.pedersen(leaves[1], leaves[0])
        assertEquals(rawHash, merkleHash)

        val leaves2 = listOf(
            Felt.fromHex("0x5bb9440e27889a364bcb678b1f679ecd1347acdedcbf36e83494f857cc58026"),
            Felt.fromHex("0x3"),
        )
        val merkleHash2 = MerkleTree.hash(leaves2[0], leaves2[1])
        val rawHash2 = StarknetCurve.pedersen(leaves2[1], leaves2[0])
        assertEquals(rawHash2, merkleHash2)
    }

    @Test
    fun `throws on 0 elements`() {
        val leaves = emptyList<Felt>()

        assertThrows<IllegalArgumentException>("Cannot build Merkle tree from an empty list of leaves.") {
            MerkleTree(leaves)
        }
    }

    @Nested
    inner class BuildFromElementsTest {
        @Test
        fun `build merkle tree from 1 element`() {
            val leaves = listOf(Felt.ONE)
            val tree = MerkleTree(leaves)

            val manualMerkleRootHash = leaves[0]

            assertEquals(0, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @Test
        fun `build merkle tree from 2 elements`() {
            val leaves = listOf(Felt.ONE, Felt.fromHex("0x2"))
            val tree = MerkleTree(leaves)

            val manualMerkleRootHash = MerkleTree.hash(leaves[0], leaves[1])

            assertEquals(0, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @Test
        fun `build merkle tree from 4 elements`() {
            val leaves = (1..4).map { Felt.fromHex("0x$it") }
            val tree = MerkleTree(leaves)

            val manualMerkleRootHash = MerkleTree.hash(
                MerkleTree.hash(leaves[0], leaves[1]),
                MerkleTree.hash(leaves[2], leaves[3]),
            )
            assertEquals(1, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @Test
        fun `build merkle tree from 6 elements`() {
            val leaves = (1..6).map { Felt.fromHex("0x$it") }
            val tree = MerkleTree(leaves)

            val manualMerkleRootHash = MerkleTree.hash(
                MerkleTree.hash(
                    MerkleTree.hash(leaves[0], leaves[1]),
                    MerkleTree.hash(leaves[2], leaves[3]),
                ),
                MerkleTree.hash(
                    MerkleTree.hash(leaves[4], leaves[5]),
                    Felt.fromHex("0x0"),
                ),
            )

            assertEquals(2, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @Test
        fun `build merkle tree from 7 elements`() {
            val leaves = (1..7).map { Felt.fromHex("0x$it") }
            val tree = MerkleTree(leaves)

            val manualMerkleRootHash = MerkleTree.hash(
                MerkleTree.hash(
                    MerkleTree.hash(leaves[0], leaves[1]),
                    MerkleTree.hash(leaves[2], leaves[3]),
                ),
                MerkleTree.hash(
                    MerkleTree.hash(leaves[4], leaves[5]),
                    MerkleTree.hash(leaves[6], Felt.ZERO),
                ),
            )

            assertEquals(2, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }
    }
}
