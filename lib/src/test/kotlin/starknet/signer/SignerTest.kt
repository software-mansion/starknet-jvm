package starknet.signer

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.signer.StarkCurveSigner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import starknet.data.loadTypedData

internal class SignerTest {
    companion object {
        val TD by lazy { loadTypedData("rev_1/typed_data_basic_types_example.json") }
    }

    private val signer = StarkCurveSigner(
        privateKey = Felt(0x123456789),
    )

    @Test
    fun signTransaction() {
        // Create calls
        val call1 = Call(
            contractAddress = Felt(0x123),
            entrypoint = "increase_balance",
            calldata = listOf(Felt(10)),
        )
        val call2 = Call(
            contractAddress = Felt(0x456),
            entrypoint = "increase_balance",
            calldata = listOf(Felt(20)),
        )

        // Create calldata
        val calldata = AccountCalldataTransformer.callsToExecuteCalldata(listOf(call1, call2), Felt(1))
        val resourceBounds = ResourceBoundsMapping(
            l1Gas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000),
                maxPricePerUnit = Uint128(10_000_000_000_000_000),
            ),
            l2Gas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000_000),
                maxPricePerUnit = Uint128(1_000_000_000_000_000_000),
            ),
            l1DataGas = ResourceBounds(
                maxAmount = Uint64(100_000_000_000),
                maxPricePerUnit = Uint128(10_000_000_000_000_000),
            ),
        )
        val tx = InvokeTransactionV3(
            senderAddress = Felt(0x789),
            calldata = calldata,
            chainId = StarknetChainId.SEPOLIA,
            nonce = Felt(0),
            resourceBounds = resourceBounds,
        )

        // Get signature
        val signature = signer.signTransaction(tx)

        // Create signed transaction
        val signedTransaction = tx.copy(signature = signer.signTransaction(tx))

        assertEquals(2, signature.size)
        assertTrue(signature.all { it != Felt.ZERO })
    }

    @Test
    fun signTypedData() {
        val accountAddress = Felt(0x123)

        // Get signature
        val signature = signer.signTypedData(
            typedData = TD,
            accountAddress = accountAddress,
        )

        assertEquals(2, signature.size)
        assertTrue(signature.all { it != Felt.ZERO })
    }
}
