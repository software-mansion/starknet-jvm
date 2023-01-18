package com.swmansion.starknet.data

import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Uint256
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CallTest {

    private val address = Felt.fromHex("0x111")
    private val selectorString = "abcdef"
    private val selector = selectorFromName(selectorString)
    private val param = Uint256.fromHex("0x210439592369239639725932969795495939596")
    private val low = param.low
    private val high = param.high
    private val calldata = listOf(low, high)

    private val expectedCall = Call(address, selector, calldata)

    @Test
    fun `create call from primary constructor`() {
        val call = Call(address, selector, listOf(low, high))

        assertEquals(expectedCall, call)
    }

    @Test
    fun `create call from string entrypoint`() {
        val call = Call(address, selectorString, listOf(low, high))

        assertEquals(expectedCall, call)
    }

    @Test
    fun `create call without calldata`() {
        val call = Call(address, selector)

        assertEquals(expectedCall.copy(calldata = emptyList()), call)
    }

    @Test
    fun `create call without calldata string entrypoint`() {
        val call = Call(address, selectorString)

        assertEquals(expectedCall.copy(calldata = emptyList()), call)
    }

    @Test
    fun `create call from call arguments`() {
        val call = Call.fromCallArguments(address, selector, listOf(param))

        assertEquals(expectedCall, call)
    }

    @Test
    fun `create call from call arguments string entrypoint`() {
        val call = Call.fromCallArguments(address, selectorString, listOf(param))

        assertEquals(expectedCall, call)
    }
}
