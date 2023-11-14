package com.example.androiddemo

import com.swmansion.starknet.data.types.Felt
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 * Use these tests to minimize execution time when your tests have no Android framework or crypto dependencies.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun `test felt`() {
        assertEquals(Felt.fromHex("0x1"), Felt.ONE)
    }
}
