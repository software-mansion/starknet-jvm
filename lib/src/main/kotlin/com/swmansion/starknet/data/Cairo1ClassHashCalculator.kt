package com.swmansion.starknet.data

import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.starknetKeccak
import com.swmansion.starknet.data.types.*

object Cairo1ClassHashCalculator {
    fun computeSierraClassHash(contract: Cairo1ContractDefinition): Felt {
        val contractClass = contract.deserializationJson.decodeFromJsonElement(ContractClass.serializer(), contract.toJson())

        val sierraVersion = Felt.fromShortString("CONTRACT_CLASS_V" + contractClass.contractClassVersion)
        val externalEntryPointHash = Poseidon.poseidonHash(getSierraEntryPointsArray(contractClass.entryPointsByType.external))
        val l1HandlerEntryPointHash = Poseidon.poseidonHash(getSierraEntryPointsArray(contractClass.entryPointsByType.l1Handler))
        val constructorEntryPointHash = Poseidon.poseidonHash(getSierraEntryPointsArray(contractClass.entryPointsByType.constructor))
        val abiHash = starknetKeccak(contractClass.abi!!.toByteArray(charset = Charsets.UTF_8))
        val sierraProgramHash = Poseidon.poseidonHash(contractClass.sierraProgram)

        return (Poseidon.poseidonHash(listOf(sierraVersion, externalEntryPointHash, l1HandlerEntryPointHash, constructorEntryPointHash, abiHash, sierraProgramHash)))
    }

    fun computeCasmClassHash(contract: CasmContractDefinition): Felt {
        val contractClass = contract.deserializationJson.decodeFromJsonElement(CasmContractClass.serializer(), contract.toJson())

        val casmVersion = Felt.fromShortString(contractClass.casmClassVersion)
        val externalEntryPointHash = Poseidon.poseidonHash(getCasmEntryPointsArray(contractClass.entryPointsByType.external))
        val l1HandlerEntryPointHash = Poseidon.poseidonHash(getCasmEntryPointsArray(contractClass.entryPointsByType.l1Handler))
        val constructorEntryPointHash = Poseidon.poseidonHash(getCasmEntryPointsArray(contractClass.entryPointsByType.constructor))
        val bytecodeHash = Poseidon.poseidonHash(contractClass.bytecode)

        return (Poseidon.poseidonHash(listOf(casmVersion, externalEntryPointHash, l1HandlerEntryPointHash, constructorEntryPointHash, bytecodeHash)))
    }

    private fun getSierraEntryPointsArray(arr: List<SierraEntryPoint>): List<Felt> {
        val entryPointsArray = mutableListOf<Felt>()
        for (ep in arr) {
            entryPointsArray.addAll(listOf(ep.selector, Felt(ep.functionIdx)))
        }
        return entryPointsArray
    }

    private fun getCasmEntryPointsArray(arr: List<CasmEntryPoint>): List<Felt> {
        val entryPointsArray = mutableListOf<Felt>()
        for (ep in arr) {
            requireNotNull(ep.builtins) { "Builtins cannot be null!" }

            val encodedBuiltins = ep.builtins.map { Felt.fromShortString(it) }
            val builtinsHash = Poseidon.poseidonHash(encodedBuiltins)

            entryPointsArray.addAll(listOf(ep.selector, Felt(ep.offset), builtinsHash))
        }
        return entryPointsArray
    }
}
