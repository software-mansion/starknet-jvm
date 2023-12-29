package network.provider

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.transactions.*
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import network.utils.NetworkConfig
import network.utils.NetworkConfig.Network
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class ProviderTest {
    companion object {
        @JvmStatic
        private val config = NetworkConfig.config
        private val network = config.network
        private val rpcUrl = config.rpcUrl

        private val provider = JsonRpcProvider(rpcUrl)
    }

    @Test
    fun `get spec version`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val request = provider.getSpecVersion()
        val specVersion = request.send()

        assertNotEquals(0, specVersion.length)
        val validPattern = "\\d+\\.\\d+\\.\\d+".toRegex()
        assertTrue(validPattern.containsMatchIn(specVersion))
    }

    @Test
    fun `get chain id`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        val request = provider.getChainId()
        val chainId = request.send()

        val expectedChainId = when (network) {
            Network.GOERLI_INTEGRATION -> StarknetChainId.GOERLI
            Network.GOERLI_TESTNET -> StarknetChainId.GOERLI
            Network.SEPOLIA_INTEGRATION -> StarknetChainId.SEPOLIA_INTEGRATION
            Network.SEPOLIA_TESTNET -> StarknetChainId.SEPOLIA_TESTNET
        }
        assertEquals(expectedChainId, chainId)
    }

    @Test
    fun `get transaction status`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x34223514e92989608e3b36f2a2a53011fa0699a275d7936a18921a11963c792")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x72776cb6462e7e1268bd93dee8ad2df5ee0abed955e3010182161bdb0daea62")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x3d381262ca26570083eab24e431b72e69ce5e33b423c034aef466613ee20511")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x03f6e39d90aa084585ac59fa4c19429499951eb7a279427bbe0a7718c8d9072f")
        }
        val transactionHash2 = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x5e2e61a59e3f254f2c65109344be985dff979abd01b9c15b659a95f466689bf")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x6bf08a6547a8be3cd3d718a068c2c0e9d3820252935f766c1ba6dd46f62e05")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0xe691cd69a7dc7efb815f6029db26cf5d7a14f2b5e893e2de4a281c82b5a0d")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x7246ea24cefc572096264f1f243cd9b32837c17324ac23076070f29f50b6393")
        }
        val transactionStatus = provider.getTransactionStatus(transactionHash).send()
        // TODO: Re-enable this assertion for integration once transaction appear as accepted on L1 again
        if (network != Network.GOERLI_INTEGRATION) {
            assertEquals(TransactionStatus.ACCEPTED_ON_L1, transactionStatus.finalityStatus)
        }
        assertNotNull(transactionStatus.executionStatus)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, transactionStatus.executionStatus)

        val transactionStatus2 = provider.getTransactionStatus(transactionHash2).send()

        assertNotNull(transactionStatus2.executionStatus)
        assertEquals(TransactionExecutionStatus.REVERTED, transactionStatus2.executionStatus)
    }

    @Disabled
    @Test
    fun `estimate message fee`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#344) Currently, Juno fails to estimate the message fee.
        assumeFalse(network == Network.GOERLI_TESTNET)

        val gasConsumed = Felt(19931)
        val gasPrice = Felt(1022979559)
        val overallFee = Felt(20389005590429)

        val fromAddress = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0xbe1259ff905cadbbaa62514388b71bdefb8aacc1")
            Network.GOERLI_TESTNET -> Felt.fromHex("0xf7d519a1660dd9237d47c039696fe4a2b93b6987")
            else -> throw NotImplementedError("Sepolia networks are not yet supported")
        }
        val toAddress = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x073314940630fd6dcda0d772d4c972c4e0a9946bef9dabf4ef84eda8ef542b82")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x0677d43766e880bfa6ddcf43e2ff54d54c64105e4a7fce20b7b1d40086a3a674")
            else -> throw NotImplementedError("Sepolia networks are not yet supported")
        }
        val selector = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x02d757788a8d8d6f21d1cd40bce38a8222d70654214e96ff95d8086e684fbee5")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x026490f901ea8ad5a245d987479919f1d20fbb0c164367e33ef09a9ea4ba8d04")
            else -> throw NotImplementedError("Sepolia networks are not yet supported")
        }
        val message = MessageL1ToL2(
            fromAddress = fromAddress,
            toAddress = toAddress,
            selector = selector,
            payload = listOf(
                Felt.fromHex("0x54d01e5fc6eb4e919ceaab6ab6af192e89d1beb4f29d916768c61a4d48e6c95"),
                Felt.fromHex("0x38d7ea4c68000"),
                Felt.fromHex("0x0"),
            ),
        )

        val request = provider.getEstimateMessageFee(
            message = message,
            blockNumber = 10000,
        )
        val response = request.send()

        assertNotNull(response)
        assertNotNull(response.gasConsumed)
        assertNotNull(response.gasPrice)
        assertNotNull(response.overallFee)

        assertEquals(gasPrice, response.gasPrice)
        assertEquals(gasConsumed, response.gasConsumed)
        assertEquals(overallFee, response.overallFee)
    }

    @Test
    fun `get deploy account v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x029da9f8997ce580718fa02ed0bd628976418b30a0c5c542510aaef21a4445e4")
            Network.GOERLI_TESTNET -> Felt.fromHex("0xa8f359bad1181a37e41479b70a5c69a34e824b90accf8fbfba022708b7f08f")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x014f73658da451dec4d14de51e12ad79b737c6342814b5f05c39da83a9ec1f3c")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x62e87178d0bf221e453276d607420fd256d928349c620f08958eeb66f24d2d9")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)
        assertEquals(Felt.ONE, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedDeployAccountTransactionReceipt)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get deploy account v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#384) Test v3 transactions on Sepolia
        assumeTrue(network == Network.GOERLI_INTEGRATION)
        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x007c1ca558aaec1a14a4c0553517013631fad81c48667a3bcd635617c2560276")
            else -> throw NotImplementedError("Test is not yet supported for this network: $network")
        }

        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)
        assertEquals(Felt(3), tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedDeployAccountTransactionReceipt)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get reverted invoke v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x5e2e61a59e3f254f2c65109344be985dff979abd01b9c15b659a95f466689bf")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x6bf08a6547a8be3cd3d718a068c2c0e9d3820252935f766c1ba6dd46f62e05")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0xe691cd69a7dc7efb815f6029db26cf5d7a14f2b5e893e2de4a281c82b5a0d")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x7246ea24cefc572096264f1f243cd9b32837c17324ac23076070f29f50b6393")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedInvokeTransactionReceipt)
        assertFalse(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.REVERTED, receipt.executionStatus)
        assertNotNull(receipt.revertReason)
    }

    @Test
    fun `get invoke v1 transaction with events`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x34223514e92989608e3b36f2a2a53011fa0699a275d7936a18921a11963c792")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x72776cb6462e7e1268bd93dee8ad2df5ee0abed955e3010182161bdb0daea62")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x3d381262ca26570083eab24e431b72e69ce5e33b423c034aef466613ee20511")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x03f6e39d90aa084585ac59fa4c19429499951eb7a279427bbe0a7718c8d9072f")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is InvokeTransaction)
        assertEquals(transactionHash, tx.hash)
        assertEquals(TransactionType.INVOKE, tx.type)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertTrue(receipt.events.size >= 2)

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        assertTrue(receipt is ProcessedInvokeTransactionReceipt)
    }

    @Test
    fun `get invoke v3 transaction with events`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#384) Test v3 transactions on Sepolia
        assumeTrue(network == Network.GOERLI_INTEGRATION)
        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x06f99b0650eb02eaf16cc97820075b1dc8c8a4ada22ef0a606f3c0b066d7ce07")
            else -> throw NotImplementedError("Test is not yet supported for this network: $network")
        }

        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is InvokeTransaction)
        assertEquals(transactionHash, tx.hash)
        assertEquals(TransactionType.INVOKE, tx.type)
        assertEquals(Felt(3), tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertTrue(receipt.events.size >= 2)

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        assertTrue(receipt is ProcessedInvokeTransactionReceipt)
    }

    @Test
    fun `get declare v0 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x6d346ba207eb124355960c19c737698ad37a3c920a588b741e0130ff5bd4d6d")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x6d346ba207eb124355960c19c737698ad37a3c920a588b741e0130ff5bd4d6d")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x76b8ab51555253c7fcbcf2f8419b2576160160daf01ffb7c882e448e24b64ff")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x656e113cb27707d2147c271a79c51d1069b0273ae447b965e15154a17b3ec01")
        }
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV0
        assertEquals(transactionHash, tx.hash)
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(Felt.ZERO, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        // TODO: Re-enable this assertion for integration once transaction appear as accepted on L1 again
        if (network != Network.GOERLI_INTEGRATION) {
            assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        }
        assertNull(receipt.revertReason)

        receipt is ProcessedDeclareTransactionReceipt
        assertEquals(Felt.ZERO, receipt.actualFee.amount)
        assertEquals(PriceUnit.WEI, receipt.actualFee.unit)
    }

    @Test
    fun `get declare v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x417ec8ece9d2d2e68307069fdcde3c1fd8b0713b8a2687b56c19455c6ea85c1")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x6801a86a4a6873f62aaa478151ba03171691edde897c434ec8cf9db3bb77573")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x5e27aad6f9139f6eeb0ee886179c40b551e91ad8bcc80e16ff0fe6d5444d6f9")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x012bc00eadd7f25a5523e5857da2073e2028070a5e616723931ac290aba3f22a")
        }
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV1
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedDeclareTransactionReceipt)
        assertTrue(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get declare v2 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x70fac6862a52000d2d63a1c845c26c9202c9030921b4607818a0820a46eab26")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x747a364442ed4d72cd24d7e26f2c6ab0bc98c0a835f2276cd2bc07266331555")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x01556b67a22bc26dfc4827286997e3e3b53380e14ba1d879f1d67b7ffbd5a808")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x0683e900e4c0e27e164f0b7569f86edec6dec919e55d97728113b40fe6b731c7")
        }
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV2
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedDeclareTransactionReceipt)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get declare v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#384) Test v3 transactions on Sepolia
        assumeTrue(network == Network.GOERLI_INTEGRATION)
        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x86693a36721bb586bee1f8c8b9ea33fbbb7f820dde48d9068dfa94a99ef53")
            else -> throw NotImplementedError("Test is not yet supported for this network: $network")
        }

        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV3
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedDeclareTransactionReceipt)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get l1 handler transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x5753d979e05f7c079b04c8fdafe2b6f4951492b6509f66f1d86e7c061882ee3")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x47ca5f1e16ba2cf997ebc33e60dfa3e5323fb3eebf63b0b9319fb4f6174ade8")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x2c678e2dda58eb4bffd1f2c45cca6a883e6f388e91aa5e153ff09e3f52a0dc5")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x785c2ada3f53fbc66078d47715c27718f92e6e48b96372b36e5197de69b82b5")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is L1HandlerTransaction)
        assertEquals(TransactionType.L1_HANDLER, tx.type)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedL1HandlerTransactionReceipt)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        val expectedMessageHash = when (network) {
            Network.GOERLI_TESTNET -> NumAsHex.fromHex("0x6411d0d085d25a8da5f53b45f616d8c8473c12d5af0e9ed84515af0a58a28bf1")
            Network.GOERLI_INTEGRATION -> NumAsHex.fromHex("0xf6359249ccef7caea9158c76133893d8bcbc09701df4caf111e7e2fc1283eb08")
            Network.SEPOLIA_INTEGRATION -> NumAsHex.fromHex("0x62aadcf51c6b5d9169523f4f62baae7a50c7fb4915b3789c044545af6e6b039c")
            Network.SEPOLIA_TESTNET -> NumAsHex.fromHex("0x42e76df4e3d5255262929c27132bd0d295a8d3db2cfe63d2fcd061c7a7a7ab34")
        }

        assertEquals(expectedMessageHash, (receipt as ProcessedL1HandlerTransactionReceipt).messageHash)
    }

    @Test
    fun `get transaction receipt with l2 to l1 messages`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x157438780a13f8cdfa5c291d666361c112ac0082751fac480e520a7bd78af6d")
            Network.GOERLI_TESTNET -> Felt.fromHex("0xd73488307e92d91ddf1a84b5670b37d3b1598e56096ad9be9925597133b681")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x9ce93eba4c0201940e229cb899fc8822a447ad48ed5e61c929b4e7c9f6ace9")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x26cda254979ce4e9705b83a886e175f5194b89339ce7de4bbae9ea6c9966c6d")
        }

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is ProcessedTransactionReceipt)
        assertTrue(receipt.isAccepted)

        assertEquals(2, receipt.messagesSent.size)
        assertNotNull(receipt.messagesSent[0].fromAddress)
        assertNotNull(receipt.messagesSent[0].toAddress)
        assertNotNull(receipt.messagesSent[0].payload)
    }

    @Test
    fun `get block with transactions with latest block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val request = provider.getBlockWithTxs(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
    }

    @Disabled
    @Test
    fun `get block with transactions with pending block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val request = provider.getBlockWithTxs(BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PendingBlockWithTransactionsResponse)
    }

    @Test
    fun `get block with transactions with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x164923d2819eb5dd207275b51348ea2ac6b46965290ffcdf89350c998f28048")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x42be1d27e55744ab5d43ee98b8feb9895e96a034d6bb742a8204f530c680f3c")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x3224282d7f577055cd72894cb66e511f105576788796a1d50538e7eae5efbe1")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x1e04294ce4f7dd489ba5b5618dc112b37f9a7e82e2ded5691fb3083839dd3b5")
        }
        val request = provider.getBlockWithTxs(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
        assertTrue(response.transactions.size >= 4)
    }

    @Test
    fun `get block with transactions with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockNumber = 1000
        val request = provider.getBlockWithTxs(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionsResponse)
        assertTrue(response.transactions.size >= 4)
    }

    @Test
    fun `get block with transaction hashes with latest block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val request = provider.getBlockWithTxHashes(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
    }

    @Disabled
    @Test
    fun `get block with transaction hashes with pending block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val request = provider.getBlockWithTxHashes(BlockTag.PENDING)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PendingBlockWithTransactionHashesResponse)
    }

    @Test
    fun `get block with transaction hashes with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockHash = when (network) {
            Network.GOERLI_INTEGRATION -> Felt.fromHex("0x164923d2819eb5dd207275b51348ea2ac6b46965290ffcdf89350c998f28048")
            Network.GOERLI_TESTNET -> Felt.fromHex("0x42be1d27e55744ab5d43ee98b8feb9895e96a034d6bb742a8204f530c680f3c")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x3224282d7f577055cd72894cb66e511f105576788796a1d50538e7eae5efbe1")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x1e04294ce4f7dd489ba5b5618dc112b37f9a7e82e2ded5691fb3083839dd3b5")
        }
        val request = provider.getBlockWithTxHashes(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
        assertTrue(response.transactionHashes.size >= 4)
    }

    @Test
    fun `get block with transaction hashes with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockNumber = 1000
        val request = provider.getBlockWithTxHashes(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is BlockWithTransactionHashesResponse)
        assertTrue(response.transactionHashes.size >= 4)
    }
}
