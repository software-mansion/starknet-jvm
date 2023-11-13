package starknet.data.types

import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class MerkleTreeTest {
    data class MerkleTest(
        val leafHashes: List<Felt>,
        val expectedBranches: Int,
        val expectedRootHash: Felt,
    )

    companion object {
        private val leaves1 = listOf(Felt.ONE)
        private val leaves2 = listOf(Felt.ONE, Felt.fromHex("0x2"))
        private val leaves4 = (1..4).map { Felt.fromHex("0x$it") }
        private val leaves6 = (1..6).map { Felt.fromHex("0x$it") }
        private val leaves7 = (1..7).map { Felt.fromHex("0x$it") }

        @JvmStatic
        fun getMerkleTest(): List<Arguments> {
            val testCases = listOf(
                MerkleTest(leaves1, 0, leaves1[0]),
                MerkleTest(leaves2, 0, MerkleTree.hash(leaves2[0], leaves2[1])),
                MerkleTest(
                    leafHashes = leaves4,
                    expectedBranches = 1,
                    expectedRootHash = MerkleTree.hash(
                        MerkleTree.hash(leaves4[0], leaves4[1]),
                        MerkleTree.hash(leaves4[2], leaves4[3]),
                    ),
                ),
                MerkleTest(
                    leafHashes = leaves6,
                    expectedBranches = 2,
                    expectedRootHash = MerkleTree.hash(
                        MerkleTree.hash(
                            MerkleTree.hash(leaves6[0], leaves6[1]),
                            MerkleTree.hash(leaves6[2], leaves6[3]),
                        ),
                        MerkleTree.hash(
                            MerkleTree.hash(leaves6[4], leaves6[5]),
                            Felt.ZERO,
                        ),
                    ),
                ),
                MerkleTest(
                    leafHashes = leaves7,
                    expectedBranches = 2,
                    expectedRootHash = MerkleTree.hash(
                        MerkleTree.hash(
                            MerkleTree.hash(leaves7[0], leaves7[1]),
                            MerkleTree.hash(leaves7[2], leaves7[3]),
                        ),
                        MerkleTree.hash(
                            MerkleTree.hash(leaves7[4], leaves7[5]),
                            MerkleTree.hash(leaves7[6], Felt.ZERO),
                        ),
                    ),
                ),
            )
            return testCases.map { Arguments.of(it.leafHashes.size, it) }
        }
    }

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
    
    @ParameterizedTest(name = "build merkle tree from {0} elements")
    @MethodSource("getMerkleTest")
    fun `build merkle tree`(leavesNumber : Int, testCase: MerkleTest) {
        val tree = MerkleTree(testCase.leafHashes)

        assertEquals(testCase.expectedBranches, tree.branches.size)
        assertEquals(testCase.expectedRootHash, tree.rootHash)
    }

    @Test
    fun `merkle tree throws on 0 leaves`() {
        val leaves = emptyList<Felt>()

        assertThrows<IllegalArgumentException>("Cannot build Merkle tree from an empty list of leaves.") {
            MerkleTree(leaves)
        }
    }
}
