@file:JvmName("Tip")

package com.swmansion.starknet.helpers

import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.extensions.map
import com.swmansion.starknet.extensions.toUint64
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.Request
import org.apache.commons.math3.stat.descriptive.rank.Median

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the specified block.
 *
 * @param provider a provider used to interact with Starknet
 * @param blockHash the block hash to estimate the tip for
 *
 * @return
 */
fun estimateTip(
    provider: Provider,
    blockHash: Felt,
): Request<Uint64> {
    return estimateTip(provider, BlockId.Hash(blockHash))
}

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the specified block.
 *
 * @param provider a provider used to interact with Starknet
 * @param blockTag the block tag to estimate the tip for
 *
 * @return
 */
fun estimateTip(
    provider: Provider,
    blockTag: BlockTag,
): Request<Uint64> {
    return estimateTip(provider, BlockId.Tag(blockTag))
}

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the specified block.
 *
 * @param provider a provider used to interact with Starknet
 * @param blockNumber the block number to estimate the tip for
 *
 * @return
 */
fun estimateTip(
    provider: Provider,
    blockNumber: Int,
): Request<Uint64> {
    return estimateTip(provider, BlockId.Number(blockNumber))
}

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the latest block.
 *
 * @param provider a provider used to interact with Starknet
 *
 * @return
 */
fun estimateTip(
    provider: Provider,
): Request<Uint64> {
    return estimateTip(provider, BlockId.Tag(BlockTag.LATEST))
}

/** Estimate the transaction tip by taking the median of all V3 transaction tips in the specified block.
 *
 * @param provider a provider used to interact with Starknet
 * @param blockId the block ID to estimate the tip for
 *
 * @return
 */
private fun estimateTip(
    provider: Provider,
    blockId: BlockId,
): Request<Uint64> {
    val request = when (blockId) {
        is BlockId.Hash -> provider.getBlockWithTxs(blockId.blockHash)
        is BlockId.Number -> provider.getBlockWithTxs(blockId.blockNumber)
        is BlockId.Tag -> provider.getBlockWithTxs(blockId.blockTag)
    }

    return request.map { blockWithTxs ->
        val tips = blockWithTxs.transactions
            .filterIsInstance<TransactionV3>()
            .map { it.tip.value }

        if (tips.isEmpty()) {
            return@map Uint64.ZERO
        }

        Median().evaluate(tips.map { it.toDouble() }.toDoubleArray()).toInt().toUint64
    }
}
