@file:JvmName("Constants")

package starknet.data

import java.math.BigInteger

val PRIME = BigInteger("800000000000011000000000000000000000000000000000000000000000001", 16)

val DEFAULT_ENTRY_POINT_NAME = "__default__"
val DEFAULT_L1_ENTRY_POINT_NAME = "__l1_default__"
val DEFAULT_ENTRY_POINT_SELECTOR = 0
val EXECUTE_ENTRY_POINT_NAME = "__execute__"
val TRANSFER_ENTRY_POINT_NAME = "transfer"
