package network.provider

import com.swmansion.starknet.data.types.*
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

        private val declareV0TransactionHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x76b8ab51555253c7fcbcf2f8419b2576160160daf01ffb7c882e448e24b64ff")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x656e113cb27707d2147c271a79c51d1069b0273ae447b965e15154a17b3ec01")
        }
        private val invokeV1TransactionHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x3d381262ca26570083eab24e431b72e69ce5e33b423c034aef466613ee20511")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x03f6e39d90aa084585ac59fa4c19429499951eb7a279427bbe0a7718c8d9072f")
        }
        private val revertedInvokeV1TransactionHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0xe691cd69a7dc7efb815f6029db26cf5d7a14f2b5e893e2de4a281c82b5a0d")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x7246ea24cefc572096264f1f243cd9b32837c17324ac23076070f29f50b6393")
        }
        private val deployAccountV1TransactionHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x014f73658da451dec4d14de51e12ad79b737c6342814b5f05c39da83a9ec1f3c")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x62e87178d0bf221e453276d607420fd256d928349c620f08958eeb66f24d2d9")
        }
        private val declareV1TransactionHash = when (network) {
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x5e27aad6f9139f6eeb0ee886179c40b551e91ad8bcc80e16ff0fe6d5444d6f9")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x012bc00eadd7f25a5523e5857da2073e2028070a5e616723931ac290aba3f22a")
        }
        private val declareV2TransactionHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x01556b67a22bc26dfc4827286997e3e3b53380e14ba1d879f1d67b7ffbd5a808")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x0683e900e4c0e27e164f0b7569f86edec6dec919e55d97728113b40fe6b731c7")
        }
        private val deployAccountV3TransactionHash by lazy {
            when (network) {
                Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x38ce58ed386612e75c64342f1df6b2ffb8d3e62adf3c9f73475527aa6483152")
                else -> throw NotImplementedError("Test is not yet supported for this network: $network")
            }
        }
        private val invokeV3TransactionHash by lazy {
            when (network) {
                Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x6e93e3758361ce0d0785c981addff8905ad7641cc07832e74f334370ffec39c")
                else -> throw NotImplementedError("Test is not yet supported for this network: $network")
            }
        }
        private val declareV3TransactionHash by lazy {
            when (network) {
                Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x7a0c820d91797e5b6196b87a36e449db146b3bfc76d40ad38184012a469a9e6")
                else -> throw NotImplementedError("Test is not yet supported for this network: $network")
            }
        }
        private val specificBlockHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x3224282d7f577055cd72894cb66e511f105576788796a1d50538e7eae5efbe1")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x1e04294ce4f7dd489ba5b5618dc112b37f9a7e82e2ded5691fb3083839dd3b5")
        }
        private val strkContractAddress =
            Felt.fromHex("0x04718f5a0fc34cc1af16a1cdee98ffb20c31f5cd61d6ab07201858f4287c938d")
        private val strkClassHash = Felt.fromHex("0x04ad3c1dc8413453db314497945b6903e1c766495a1e60492d44da9c2a986e4b")

        @Suppress("const")
        private val specificBlockNumber = 1000
    }

    @Test
    fun `get spec version`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val request = provider.getSpecVersion()
        val specVersion = request.send().value

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
            Network.SEPOLIA_INTEGRATION -> StarknetChainId.INTEGRATION_SEPOLIA
            Network.SEPOLIA_TESTNET -> StarknetChainId.SEPOLIA
        }
        assertEquals(expectedChainId, chainId)
    }

    @Test
    fun `get transaction status`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = invokeV1TransactionHash
        val transactionHash2 = revertedInvokeV1TransactionHash
        val transactionStatus = provider.getTransactionStatus(transactionHash).send()

        assertEquals(TransactionStatus.ACCEPTED_ON_L1, transactionStatus.finalityStatus)
        assertNotNull(transactionStatus.executionStatus)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, transactionStatus.executionStatus)

        val transactionStatus2 = provider.getTransactionStatus(transactionHash2).send()

        assertNotNull(transactionStatus2.executionStatus)
        assertEquals(TransactionExecutionStatus.REVERTED, transactionStatus2.executionStatus)
    }

    @Test
    fun `get deploy account v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = deployAccountV1TransactionHash
        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)
        assertEquals(TransactionVersion.V1, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is DeployAccountTransactionReceipt)
        assertFalse(receipt.isPending)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get deploy account v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#384) Test v3 transactions on Sepolia

        val transactionHash = deployAccountV3TransactionHash

        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)
        assertEquals(TransactionVersion.V3, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is DeployAccountTransactionReceipt)
        assertFalse(receipt.isPending)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get reverted invoke v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = revertedInvokeV1TransactionHash
        val tx = provider.getTransaction(transactionHash).send()
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is InvokeTransactionReceipt)
        assertFalse(receipt.isPending)
        assertFalse(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.REVERTED, receipt.executionStatus)
        assertNotNull(receipt.revertReason)
    }

    @Test
    fun `get invoke v1 transaction with events`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = invokeV1TransactionHash
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

        assertTrue(receipt is InvokeTransactionReceipt)
        assertFalse(receipt.isPending)
    }

    @Test
    fun `get invoke v3 transaction with events`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        // TODO: (#384) Test v3 transactions on Sepolia

        val transactionHash = invokeV3TransactionHash

        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is InvokeTransaction)
        assertEquals(transactionHash, tx.hash)
        assertEquals(TransactionType.INVOKE, tx.type)
        assertEquals(TransactionVersion.V3, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertTrue(receipt.events.isNotEmpty())

        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        assertTrue(receipt is InvokeTransactionReceipt)
        assertFalse(receipt.isPending)
    }

    @Test
    fun `get declare v0 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = declareV0TransactionHash
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV0
        assertEquals(transactionHash, tx.hash)
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(TransactionVersion.V0, tx.version)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt.isAccepted)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)

        receipt is DeclareTransactionReceipt
        assertFalse(receipt.isPending)
        assertEquals(Felt.ZERO, receipt.actualFee.amount)
        assertEquals(PriceUnit.WEI, receipt.actualFee.unit)
    }

    @Test
    fun `get declare v1 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = declareV1TransactionHash
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV1
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is DeclareTransactionReceipt)
        assertFalse(receipt.isPending)
        assertTrue(receipt.isAccepted)
        assertEquals(TransactionExecutionStatus.SUCCEEDED, receipt.executionStatus)
        assertEquals(TransactionFinalityStatus.ACCEPTED_ON_L1, receipt.finalityStatus)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get declare v2 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = declareV2TransactionHash
        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV2
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is DeclareTransactionReceipt)
        assertFalse(receipt.isPending)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get declare v3 transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        val transactionHash = declareV3TransactionHash

        val tx = provider.getTransaction(transactionHash).send() as DeclareTransactionV3
        assertNotEquals(Felt.ZERO, tx.classHash)
        assertEquals(transactionHash, tx.hash)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is DeclareTransactionReceipt)
        assertFalse(receipt.isPending)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)
    }

    @Test
    fun `get l1 handler transaction`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x2c678e2dda58eb4bffd1f2c45cca6a883e6f388e91aa5e153ff09e3f52a0dc5")
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x785c2ada3f53fbc66078d47715c27718f92e6e48b96372b36e5197de69b82b5")
        }
        val tx = provider.getTransaction(transactionHash).send()
        assertTrue(tx is L1HandlerTransaction)
        assertEquals(TransactionType.L1_HANDLER, tx.type)

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertTrue(receipt is L1HandlerTransactionReceipt)
        assertFalse(receipt.isPending)
        assertTrue(receipt.isAccepted)
        assertNull(receipt.revertReason)

        val expectedMessageHash = when (network) {
            Network.SEPOLIA_INTEGRATION -> NumAsHex.fromHex("0x62aadcf51c6b5d9169523f4f62baae7a50c7fb4915b3789c044545af6e6b039c")
            Network.SEPOLIA_TESTNET -> NumAsHex.fromHex("0x42e76df4e3d5255262929c27132bd0d295a8d3db2cfe63d2fcd061c7a7a7ab34")
        }

        assertEquals(expectedMessageHash, (receipt as L1HandlerTransactionReceipt).messageHash)
    }

    @Test
    fun `get transaction receipt with l2 to l1 messages`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val transactionHash = when (network) {
            Network.SEPOLIA_TESTNET -> Felt.fromHex("0x9ce93eba4c0201940e229cb899fc8822a447ad48ed5e61c929b4e7c9f6ace9")
            Network.SEPOLIA_INTEGRATION -> Felt.fromHex("0x26cda254979ce4e9705b83a886e175f5194b89339ce7de4bbae9ea6c9966c6d")
        }

        val receiptRequest = provider.getTransactionReceipt(transactionHash)
        val receipt = receiptRequest.send()

        assertFalse(receipt.isPending)
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
        assertTrue(response is ProcessedBlockWithTransactions)
    }

//    @Disabled
    @Test
    fun `get block with transactions with pre-confirmed block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val request = provider.getBlockWithTxs(BlockTag.PRE_CONFIRMED)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PreConfirmedBlockWithTransactions)
    }

    @Test
    fun `get block with transactions with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockHash = specificBlockHash
        val request = provider.getBlockWithTxs(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithTransactions)
        assertTrue(response.transactions.size >= 4)
    }

    @Test
    fun `get block with transactions with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockNumber = specificBlockNumber
        val request = provider.getBlockWithTxs(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithTransactions)
        assertTrue(response.transactions.size >= 4)
    }

    @Test
    fun `get block with transaction hashes with latest block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val request = provider.getBlockWithTxHashes(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithTransactionHashes)
    }

    @Disabled
    @Test
    fun `get block with transaction hashes with pre-confirmed block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val request = provider.getBlockWithTxHashes(BlockTag.PRE_CONFIRMED)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PreConfirmedBlockWithTransactionHashes)
    }

    @Test
    fun `get block with transaction hashes with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockHash = specificBlockHash
        val request = provider.getBlockWithTxHashes(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithTransactionHashes)
        assertTrue(response.transactionHashes.size >= 4)
    }

    @Test
    fun `get block with transaction hashes with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockNumber = specificBlockNumber
        val request = provider.getBlockWithTxHashes(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithTransactionHashes)
        assertTrue(response.transactionHashes.size >= 4)
    }

    @Test
    fun `get block with receipts with latest block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val request = provider.getBlockWithReceipts(BlockTag.LATEST)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithReceipts)
    }

    @Disabled
    @Test
    fun `get block with receipts with pre-confirmed block tag`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))
        // Note to future developers experiencing failures in this test:
        // 1. This test may fail because there's temporarily no pending block at the moment.
        // If this happens, try running the test again after a while or disable it.
        // 2. The node can be configured such way that accessing pending block is not supported.

        val request = provider.getBlockWithReceipts(BlockTag.PRE_CONFIRMED)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is PreConfirmedBlockWithReceipts)
        response.transactionsWithReceipts.forEach {
            assertTrue(it.receipt.isPending)
        }
    }

    @Test
    fun `get block with receipts with block hash`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockHash = specificBlockHash
        val request = provider.getBlockWithReceipts(blockHash)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithReceipts)
        assertTrue(response.transactionsWithReceipts.size >= 4)
        response.transactionsWithReceipts.forEach {
            assertFalse(it.receipt.isPending)
        }
    }

    @Test
    fun `get block with receipts with block number`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val blockNumber = specificBlockNumber
        val request = provider.getBlockWithReceipts(blockNumber)
        val response = request.send()

        assertNotNull(response)
        assertTrue(response is ProcessedBlockWithReceipts)
        assertTrue(response.transactionsWithReceipts.size >= 4)
        response.transactionsWithReceipts.forEach {
            assertFalse(it.receipt.isPending)
        }
    }

    @Test
    fun `get storage proof`() {
        assumeTrue(NetworkConfig.isTestEnabled(requiresGas = false))

        val erc20BalancesKey = Felt.fromHex("0x45524332305f62616c616e636573")
        val request = provider.getStorageProof(
            blockId = BlockId.Number(556669),
            contractAddresses = listOf(strkContractAddress),
            contractsStorageKeys = listOf(
                ContractsStorageKeys(
                    contractAddress = strkContractAddress,
                    storageKeys = listOf(erc20BalancesKey),
                ),
            ),
            classHashes = listOf(strkClassHash),
        )
        val storageProof = request.send()

        assertNotNull(storageProof)
        assertEquals(17, storageProof.classesProof.size)
        assertEquals(20, storageProof.contractsProof.nodes.size)
        assertEquals(16, storageProof.contractsStorageProofs[0].size)
        assertEquals(
            Felt.fromHex("0x404446e37fc08c0bf4979821e50bdac7919b56d19d2df9e16f0aa7a0d506e50"),
            storageProof.globalRoots.blockHash,
        )
        assertEquals(
            Felt.fromHex("0x43568bf995aacf4b56615e97b7237c1b03d199344ad66d38f38fda250ef1586"),
            storageProof.globalRoots.classesTreeRoot,
        )
        assertEquals(
            Felt.fromHex("0x2ae204c3378558b33c132f4721612285d9988cc8dc99f47fce92adc6b38a189"),
            storageProof.globalRoots.contractsTreeRoot,
        )
    }
}
