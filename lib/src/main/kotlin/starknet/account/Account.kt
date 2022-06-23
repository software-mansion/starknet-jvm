package starknet.account

import starknet.data.types.Call
import starknet.data.types.ExecutionParams
import starknet.data.types.InvokeTransaction
import starknet.provider.Provider
import types.Felt

interface Account : Provider {
    val address: Felt

    fun sign(call: Call, params: ExecutionParams): InvokeTransaction {
        return sign(listOf(call), params)
    }

    // TODO: ABI?
    abstract fun sign(calls: List<Call>, params: ExecutionParams): InvokeTransaction
}

