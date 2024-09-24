package com.example.androiddemo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.Poseidon
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.crypto.starknetKeccak
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.exceptions.RpcRequestFailedException
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.signer.StarkCurveSigner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    // Create a provider for interacting with Starknet
    private val provider = JsonRpcProvider(
        url = BuildConfig.DEMO_RPC_URL,
    )
    private val scope = CoroutineScope(Dispatchers.IO)

    // Starknet ERC-20 ETH contract address (Devnet/Mainnet/Testnet/Integration)
    private val erc20ContractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val balanceValue = findViewById<TextView>(R.id.BALANCE_VALUE)
        val transactionHashValue = findViewById<TextView>(R.id.TRANSACTION_HASH_VALUE)
        val hashView = findViewById<TextView>(R.id.HASH_VIEW)
        val receiptExecutionStatusValue = findViewById<TextView>(R.id.RECEIPT_EXECUTION_STATUS_VALUE)
        val receiptActualFeeValue = findViewById<TextView>(R.id.RECEIPT_ACTUAL_FEE_VALUE)
        val receiptRevertReasonValue = findViewById<TextView>(R.id.RECEIPT_REVERT_REASON_VALUE)

        val accountAddressInput = findViewById<TextInputEditText>(R.id.ACCOUNT_ADDRESS_INPUT)
        val privateKeyInput = findViewById<TextInputEditText>(R.id.PRIVATE_KEY_INPUT)
        val recipientAddressInput = findViewById<TextInputEditText>(R.id.RECIPIENT_ADDRESS_INPUT)
        val amountInput = findViewById<TextInputEditText>(R.id.AMOUNT_INPUT)

        val refreshBalanceButton = findViewById<Button>(R.id.REFRESH_BALANCE_BUTTON)
        val transferButton = findViewById<Button>(R.id.TRANSFER_BUTTON)
        val refreshReceiptButton = findViewById<Button>(R.id.REFRESH_RECEIPT_BUTTON)

        // Pre-set account details
        accountAddressInput.setText(BuildConfig.DEMO_ACCOUNT_ADDRESS)
        privateKeyInput.setText(BuildConfig.DEMO_PRIVATE_KEY)
        recipientAddressInput.setText(BuildConfig.DEMO_RECIPIENT_ACCOUNT_ADDRESS)

        // Calculate hashes using crypto libs and display them in the UI
        val pedersen = StarknetCurve.pedersen(Felt(1), Felt(2))
        val keccak = starknetKeccak("123".toByteArray())
        val poseidon = Poseidon.poseidonHash(listOf(Felt(1), Felt(2)))
        hashView.text = """
            Pedersen: $pedersen
            Keccak: $keccak
            Poseidon: $poseidon
        """.trimIndent()

        refreshBalanceButton.setOnClickListener {
            scope.launch {
                // Catch any errors and display message in the UI
                try {
                    val accountAddress = Felt.fromHex(accountAddressInput.text.toString())

                    // Get the balance of the account
                    val balance = getBalance(accountAddress)
                    withContext(Dispatchers.Main) { balanceValue.text = "${balance.value} wei" }
                } catch (e: RpcRequestFailedException) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "${e.code}: ${e.message}", Toast.LENGTH_LONG).show() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show() }
                }
            }
        }

        transferButton.setOnClickListener {
            scope.launch {
                try {
                    // Create an account interface
                    val privateKey = Felt.fromHex(privateKeyInput.text.toString())
                    val accountAddress = Felt.fromHex(accountAddressInput.text.toString())

                    val signer = StarkCurveSigner(privateKey)
                    val chainId = provider.getChainId().sendAsync().await()
                    val account = StandardAccount(
                        address = accountAddress,
                        signer = signer,
                        provider = provider,
                        chainId = chainId,
                    )

                    // Details of the transfer transaction
                    val recipientAddress = Felt.fromHex(recipientAddressInput.text.toString())
                    val amount = Uint256(amountInput.text.toString().toBigInteger())

                    // Send the transfer transaction and get the transaction hash
                    val transferResult = transferFunds(account, recipientAddress, amount)
                    val transactionHash = transferResult.transactionHash

                    // Display the transaction hash in the UI, reset other transaction details
                    withContext(Dispatchers.Main) {
                        transactionHashValue.text = transactionHash.hexString()
                        receiptExecutionStatusValue.text = getString(R.string.not_available)
                        receiptActualFeeValue.text = getString(R.string.not_available)
                        receiptRevertReasonValue.text = getString(R.string.not_available)
                    }
                } catch (e: RpcRequestFailedException) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "${e.code}: ${e.message}", Toast.LENGTH_LONG).show() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show() }
                }
            }
        }

        refreshReceiptButton.setOnClickListener {
            scope.launch {
                try {
                    // Get the balance of the account
                    val transactionHash = Felt.fromHex(transactionHashValue.text.toString())
                    val receipt = getReceipt(transactionHash)

                    // Display the receipt details in the UI
                    withContext(Dispatchers.Main) {
                        receiptExecutionStatusValue.text = receipt.executionStatus.toString()
                        receiptActualFeeValue.text = receipt.actualFee.let { "${it.amount.value} ${it.unit.toString().lowercase()}" }
                        receiptRevertReasonValue.text = receipt.revertReason ?: getString(R.string.not_available)
                    }
                } catch (e: RpcRequestFailedException) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "${e.code}: ${e.message}", Toast.LENGTH_LONG).show() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    private suspend fun getBalance(accountAddress: Felt): Uint256 {
        // Create a call to Starknet ERC-20 ETH contract
        val call = Call(
            contractAddress = erc20ContractAddress,
            entrypoint = "balanceOf", // entrypoint can be passed both as a string name and Felt value
            calldata = listOf(accountAddress), // calldata is List<Felt>, so we wrap accountAddress in listOf()
        )

        // Create a Request object which has to be executed in synchronous or asynchronous way
        val request = provider.callContract(call)

        // Execute a Request. This operation returns JVM CompletableFuture
        val future = request.sendAsync()

        // Await the completion of the future without blocking the main thread
        // this comes from kotlinx-coroutines-jdk8
        // The result of the future is a List<Felt> which represents the output values of the balanceOf function
        val response = future.await()

        // Output value's type is UInt256 and is represented by two Felt values
        return Uint256(
            low = response[0],
            high = response[1],
        )
    }

    private suspend fun transferFunds(account: Account, recipientAddress: Felt, amount: Uint256): InvokeFunctionResponse {
        // Calldata required for transfer function includes the recipient address and the amount to transfer
        // Amount is UInt256 and is represented by two Felt values
        val calldata = listOf(recipientAddress) + amount.toCalldata()
        val call = Call(
            contractAddress = erc20ContractAddress,
            entrypoint = "transfer",
            calldata = calldata,
        )
        val request = account.executeV3(call)
        val future = request.sendAsync()
        return future.await()
    }

    private suspend fun getReceipt(transactionHash: Felt): TransactionReceipt {
        val request = provider.getTransactionReceipt(transactionHash)
        val future = request.sendAsync()

        return future.await()
    }
}
