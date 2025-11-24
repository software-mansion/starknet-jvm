package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.HashMethod
import com.swmansion.starknet.data.types.CasmContractDefinition
import com.swmansion.starknet.data.types.Felt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import kotlin.io.path.readText

internal class ClassHashCalculatorTest {

    companion object {
        @JvmStatic
        private fun computeCasmClassHashArgumentsPoseidon() = listOf(
            Arguments.of(
                "src/test/resources/contracts_v2_6/target/release/contracts_v2_6_HelloStarknet.compiled_contract_class.json",
                Felt.fromHex("0x1725af24fbfa8050f4514651990b30e06bb9993e4e5c1051206f1bef218b1c6"),
            ),
            Arguments.of(
                "src/test/resources/contracts_v2_6/precomplied/starknet_contract_v2_6.casm.json",
                Felt.fromHex("0x603dd72504d8b0bc54df4f1102fdcf87fc3b2b94750a9083a5876913eec08e4"),
            ),
        )

        @JvmStatic
        private fun computeCasmClassHashArgumentsBlake() = listOf(
            Arguments.of(
                "src/test/resources/contracts_v2_6/target/release/contracts_v2_6_HelloStarknet.compiled_contract_class.json",
                Felt.fromHex("0x5b520a1ecc05bae27e9e74aadea2866b4208f45c3689206386e687340d95165"),
            ),
            Arguments.of(
                "src/test/resources/contracts_v2_6/precomplied/starknet_contract_v2_6.casm.json",
                Felt.fromHex("0xf8c27dd667e50ba127e5e0e469381606ffece27d8c5148548b6bbc4cacf717"),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("computeCasmClassHashArgumentsPoseidon")
    fun `calculate compiled class hash cairo 2_6_0 casm 1_5_0 with poseidon`(sourcePath: String, expectedHash: Felt) {
        val casmCode = Path.of(sourcePath).readText()
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition, HashMethod.POSEIDON)
        assertEquals(expectedHash, compiledClassHash)
    }

    @ParameterizedTest
    @MethodSource("computeCasmClassHashArgumentsBlake")
    fun `calculate compiled class hash cairo 2_6_0 casm 1_5_0 with blake`(sourcePath: String, expectedHash: Felt) {
        val casmCode = Path.of(sourcePath).readText()
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition, HashMethod.BLAKE2S)
        assertEquals(expectedHash, compiledClassHash)
    }
}
