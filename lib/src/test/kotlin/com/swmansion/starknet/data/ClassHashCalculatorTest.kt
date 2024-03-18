package com.swmansion.starknet.data

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
        private fun computeCasmClassHashArguments() = listOf(
            Arguments.of(
                "src/test/resources/contracts_v2_6/target/release/contracts_v2_6_HelloStarknet.compiled_contract_class.json",
                Felt.fromHex("0x1725af24fbfa8050f4514651990b30e06bb9993e4e5c1051206f1bef218b1c6"),
            ),
            Arguments.of(
                "src/test/resources/contracts_v2_6/precomplied/starknet_contract_v2_6.casm.json",
                Felt.fromHex("0x603dd72504d8b0bc54df4f1102fdcf87fc3b2b94750a9083a5876913eec08e4"),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("computeCasmClassHashArguments")
    fun `calculate compiled class hash cairo 2_6_0 casm 1_5_0`(sourcePath: String, expectedHash: Felt) {
        val casmCode = Path.of(sourcePath).readText()
        val casmContractDefinition = CasmContractDefinition(casmCode)

        val compiledClassHash = Cairo1ClassHashCalculator.computeCasmClassHash(casmContractDefinition)
        assertEquals(expectedHash, compiledClassHash)
    }
}
