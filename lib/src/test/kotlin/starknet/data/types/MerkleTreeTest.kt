package starknet.data.types

import com.swmansion.starknet.crypto.HashMethod
import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.MerkleTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class MerkleTreeTest {
    @ParameterizedTest
    @EnumSource(HashMethod::class)
    fun `calculate hashes`(hashFunction: HashMethod) {
        val applyHash: (Felt, Felt) -> Felt = when (hashFunction) {
            HashMethod.PEDERSEN -> StarknetCurve::pedersen
            HashMethod.POSEIDON -> Poseidon::poseidonHash
        }

        val leaves = listOf(
            Felt.fromHex("0x12"),
            Felt.fromHex("0xa"),
        )
        val merkleHash = MerkleTree.hash(leaves[0], leaves[1], hashFunction)
        val rawHash = applyHash(leaves[1], leaves[0])
        assertEquals(rawHash, merkleHash)

        val leaves2 = listOf(
            Felt.fromHex("0x5bb9440e27889a364bcb678b1f679ecd1347acdedcbf36e83494f857cc58026"),
            Felt.fromHex("0x3"),
        )
        val merkleHash2 = MerkleTree.hash(leaves2[0], leaves2[1], hashFunction)
        val rawHash2 = applyHash(leaves2[1], leaves2[0])
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
        @ParameterizedTest
        @EnumSource(HashMethod::class)
        fun `build merkle tree from 1 element`(hashFunction: HashMethod) {
            val leaves = listOf(Felt.ONE)
            val tree = MerkleTree(leaves, hashFunction)

            val manualMerkleRootHash = leaves[0]

            assertEquals(0, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @ParameterizedTest
        @EnumSource(HashMethod::class)
        fun `build merkle tree from 2 elements`(hashFunction: HashMethod) {
            val leaves = listOf(Felt.ONE, Felt.fromHex("0x2"))
            val tree = MerkleTree(leaves, hashFunction)

            val manualMerkleRootHash = MerkleTree.hash(leaves[0], leaves[1], hashFunction)

            assertEquals(0, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @ParameterizedTest
        @EnumSource(HashMethod::class)
        fun `build merkle tree from 4 elements`(hashFunction: HashMethod) {
            val leaves = (1..4).map { Felt.fromHex("0x$it") }
            val tree = MerkleTree(leaves, hashFunction)

            val manualMerkleRootHash = MerkleTree.hash(
                MerkleTree.hash(leaves[0], leaves[1], hashFunction),
                MerkleTree.hash(leaves[2], leaves[3], hashFunction),
                hashFunction,
            )
            assertEquals(1, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @ParameterizedTest
        @EnumSource(HashMethod::class)
        fun `build merkle tree from 6 elements`(hashFunction: HashMethod) {
            val leaves = (1..6).map { Felt.fromHex("0x$it") }
            val tree = MerkleTree(leaves, hashFunction)

            val manualMerkleRootHash = MerkleTree.hash(
                MerkleTree.hash(
                    MerkleTree.hash(leaves[0], leaves[1], hashFunction),
                    MerkleTree.hash(leaves[2], leaves[3], hashFunction),
                    hashFunction,
                ),
                MerkleTree.hash(
                    MerkleTree.hash(leaves[4], leaves[5], hashFunction),
                    Felt.ZERO,
                    hashFunction,
                ),
                hashFunction,
            )

            assertEquals(2, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }

        @ParameterizedTest
        @EnumSource(HashMethod::class)
        fun `build merkle tree from 7 elements`(hashFunction: HashMethod) {
            val leaves = (1..7).map { Felt.fromHex("0x$it") }
            val tree = MerkleTree(leaves, hashFunction)

            val manualMerkleRootHash = MerkleTree.hash(
                MerkleTree.hash(
                    MerkleTree.hash(leaves[0], leaves[1], hashFunction),
                    MerkleTree.hash(leaves[2], leaves[3], hashFunction),
                    hashFunction,
                ),
                MerkleTree.hash(
                    MerkleTree.hash(leaves[4], leaves[5], hashFunction),
                    MerkleTree.hash(leaves[6], Felt.ZERO, hashFunction),
                    hashFunction,
                ),
                hashFunction,
            )

            assertEquals(2, tree.branches.size)
            assertEquals(manualMerkleRootHash, tree.rootHash)
        }
    }
}
