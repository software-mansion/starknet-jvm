package starknet.data

import com.swmansion.starknet.data.types.Felt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SelectorTest {
    @Test
    fun selectorFromName() {
        assertEquals(
            com.swmansion.starknet.data.selectorFromName("test"),
            Felt.fromHex("0x22ff5f21f0b81b113e63f7db6da94fedef11b2119b4088b89664fb9a3cb658"),
        )
        assertEquals(
            com.swmansion.starknet.data.selectorFromName("initialize"),
            Felt.fromHex("0x79dc0da7c54b95f10aa182ad0a46400db63156920adb65eca2654c0945a463"),
        )
        assertEquals(
            com.swmansion.starknet.data.selectorFromName("mint"),
            Felt.fromHex("0x2f0b3c5710379609eb5495f1ecd348cb28167711b73609fe565a72734550354"),
        )
        assertEquals(
            com.swmansion.starknet.data.selectorFromName("__default__"),
            Felt.fromHex("0x0"),
        )
        assertEquals(
            com.swmansion.starknet.data.selectorFromName("__l1_default__"),
            Felt.fromHex("0x0"),
        )
    }
}
