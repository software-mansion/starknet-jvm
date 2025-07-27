@file:JvmName("Tip")

package com.swmansion.starknet.helpers

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.toUint64
import com.swmansion.starknet.provider.Provider
import org.nield.kotlinstatistics.median

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the latest block.
 *
 * @param provider a provider used to interact with Starknet
 *
 * @return
 */
fun estimateTip(
    provider: Provider,
): Uint64 {
    return estimateTip(provider, BlockId.Tag(BlockTag.LATEST))
}

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the specified block.
 *
 * @param provider a provider used to interact with Starknet
 * @param blockId the block ID to estimate the tip for
 *
 * @return
 */
fun estimateTip(
    provider: Provider,
    blockId: BlockId,
): Uint64 {
    val request = when (blockId) {
        is BlockId.Hash -> provider.getBlockWithTxs(blockId.blockHash)
        is BlockId.Number -> provider.getBlockWithTxs(blockId.blockNumber)
        is BlockId.Tag -> provider.getBlockWithTxs(blockId.blockTag)
    }
    val blockWithTxs = request.send()

    val tips = blockWithTxs.transactions
        .filterIsInstance<TransactionV3>()
        .map { it.tip.value.toDouble() }


    if (tips.isEmpty()) {
        return Uint64.ZERO
    }

    return tips.median().toInt().toUint64
}
