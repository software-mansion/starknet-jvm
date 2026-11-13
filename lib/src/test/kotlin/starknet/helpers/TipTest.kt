package starknet.helpers

import com.swmansion.starknet.data.types.Uint64
import com.swmansion.starknet.helpers.estimateTip
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import com.swmansion.starknet.service.http.HttpResponse
import com.swmansion.starknet.service.http.HttpService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.math.BigInteger

class TipTest {
    @Test
    fun `block without transactions`() {
        val mockedResponse =
            """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "result": {
                    "block_hash": "0x1",
                    "block_number": 111,
                    "l1_da_mode": "BLOB",
                    "l1_data_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l1_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l2_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "new_root": "0x1",
                    "parent_hash": "0x1",
                    "sequencer_address": "0x1",
                    "starknet_version": "0.14.0",
                    "event_commitment": "0x1",
                    "transaction_commitment": "0x1",
                    "receipt_commitment": "0x1",
                    "state_diff_commitment": "0x1",
                    "event_count": 1,
                    "transaction_count": 1,
                    "state_diff_length": 1,
                    "status": "ACCEPTED_ON_L2",
                    "timestamp": 123,
                    "transactions": []
                }
            }
            """.trimIndent()

        val httpService =
            mock<HttpService> {
                on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
            }
        val provider = JsonRpcProvider("", httpService)
        val tipEstimate = estimateTip(provider).send()

        assertEquals(Uint64.ZERO, tipEstimate)
    }

    @Test
    fun `odd transactions number`() {
        // 3 transactions, tips: 100, 200, 300
        val mockedResponse =
            """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "result": {
                    "block_hash": "0x1",
                    "block_number": 111,
                    "l1_da_mode": "BLOB",
                    "l1_data_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l1_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l2_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "new_root": "0x1",
                    "parent_hash": "0x1",
                    "sequencer_address": "0x1",
                    "starknet_version": "0.14.0",
                    "event_commitment": "0x1",
                    "transaction_commitment": "0x1",
                    "receipt_commitment": "0x1",
                    "state_diff_commitment": "0x1",
                    "event_count": 1,
                    "transaction_count": 1,
                    "state_diff_length": 1,
                    "status": "ACCEPTED_ON_L2",
                    "timestamp": 123,
                    "transactions": [
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x64",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0xc8",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x12c",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        }
                    ]
                }
            }
            """.trimIndent()

        val httpService =
            mock<HttpService> {
                on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
            }
        val provider = JsonRpcProvider("", httpService)
        val tipEstimate = estimateTip(provider).send()

        assertEquals(Uint64(200), tipEstimate)
    }

    @Test
    fun `even transactions number`() {
        // 4 transactions, tips: 100, 200, 300, 400
        val mockedResponse =
            """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "result": {
                    "block_hash": "0x1",
                    "block_number": 111,
                    "l1_da_mode": "BLOB",
                    "l1_data_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l1_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l2_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "new_root": "0x1",
                    "parent_hash": "0x1",
                    "sequencer_address": "0x1",
                    "starknet_version": "0.14.0",
                    "event_commitment": "0x1",
                    "transaction_commitment": "0x1",
                    "receipt_commitment": "0x1",
                    "state_diff_commitment": "0x1",
                    "event_count": 1,
                    "transaction_count": 1,
                    "state_diff_length": 1,
                    "status": "ACCEPTED_ON_L2",
                    "timestamp": 123,
                    "transactions": [
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x64",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0xc8",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x12c",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x190",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        }
                    ]
                }
            }
            """.trimIndent()

        val httpService =
            mock<HttpService> {
                on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
            }
        val provider = JsonRpcProvider("", httpService)
        val tipEstimate = estimateTip(provider).send()

        assertEquals(Uint64(250), tipEstimate)
    }

    @Test
    fun `block with old transactions`() {
        // 3 transactions v3, tips: 100, 200, 300
        // 1 transaction v1
        val mockedResponse =
            """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "result": {
                    "block_hash": "0x1",
                    "block_number": 111,
                    "l1_da_mode": "BLOB",
                    "l1_data_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l1_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l2_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "new_root": "0x1",
                    "parent_hash": "0x1",
                    "sequencer_address": "0x1",
                    "starknet_version": "0.14.0",
                    "event_commitment": "0x1",
                    "transaction_commitment": "0x1",
                    "receipt_commitment": "0x1",
                    "state_diff_commitment": "0x1",
                    "event_count": 1,
                    "transaction_count": 1,
                    "state_diff_length": 1,
                    "status": "ACCEPTED_ON_L2",
                    "timestamp": 123,
                    "transactions": [
                        {
                            "calldata": [],
                            "nonce": "0x1",
                            "max_fee":  "0x123",
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x1"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x64",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0xc8",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        },
                        {
                            "account_deployment_data": [],
                            "calldata": [],
                            "fee_data_availability_mode": "L1",
                            "nonce": "0x1",
                            "nonce_data_availability_mode": "L1",
                            "paymaster_data": [],
                            "resource_bounds": {
                                "l1_data_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l1_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                },
                                "l2_gas": {
                                    "max_amount": "0x1",
                                    "max_price_per_unit": "0x1"
                                }
                            },
                            "sender_address": "0x1",
                            "signature": [
                                "0x1",
                                "0x1"
                            ],
                            "tip": "0x12c",
                            "transaction_hash": "0x1",
                            "type": "INVOKE",
                            "version": "0x3"
                        }
                    ]
                }
            }
            """.trimIndent()

        val httpService =
            mock<HttpService> {
                on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
            }
        val provider = JsonRpcProvider("", httpService)
        val tipEstimate = estimateTip(provider).send()

        assertEquals(Uint64(200), tipEstimate)
    }

    @Test
    fun `tips exceeding int range`() {
        // tips: 10^10, 2 * 10^10, 3 * 10^10
        val tipEstimate = estimateTipForTips("0x2540be400", "0x4a817c800", "0x6fc23ac00")

        assertEquals(Uint64(20_000_000_000L), tipEstimate)
    }

    @Test
    fun `tips above 2^53`() {
        // tips: 2^53 + 1, 2^53 + 3, 2^53 + 5; none of them is representable as a Double
        val tipEstimate = estimateTipForTips("0x20000000000001", "0x20000000000003", "0x20000000000005")

        assertEquals(Uint64(BigInteger.valueOf(2).pow(53) + BigInteger.valueOf(3)), tipEstimate)
    }

    @Test
    fun `tips near u64 limit`() {
        // tips: 2^64 - 5, 2^64 - 4, 2^64 - 3, 2^64 - 2 (Uint64.MAX - 1, the largest value Uint64 accepts)
        val tipEstimate =
            estimateTipForTips(
                "0xfffffffffffffffb",
                "0xfffffffffffffffc",
                "0xfffffffffffffffd",
                "0xfffffffffffffffe",
            )

        // (2^64 - 4 + 2^64 - 3) / 2, rounded down
        assertEquals(Uint64(BigInteger.valueOf(2).pow(64) - BigInteger.valueOf(4)), tipEstimate)
    }

    private fun estimateTipForTips(vararg tips: String): Uint64 {
        val transactions =
            tips.joinToString(",") { tip ->
                """
                {
                    "account_deployment_data": [],
                    "calldata": [],
                    "fee_data_availability_mode": "L1",
                    "nonce": "0x1",
                    "nonce_data_availability_mode": "L1",
                    "paymaster_data": [],
                    "resource_bounds": {
                        "l1_data_gas": {
                            "max_amount": "0x1",
                            "max_price_per_unit": "0x1"
                        },
                        "l1_gas": {
                            "max_amount": "0x1",
                            "max_price_per_unit": "0x1"
                        },
                        "l2_gas": {
                            "max_amount": "0x1",
                            "max_price_per_unit": "0x1"
                        }
                    },
                    "sender_address": "0x1",
                    "signature": [
                        "0x1",
                        "0x1"
                    ],
                    "tip": "$tip",
                    "transaction_hash": "0x1",
                    "type": "INVOKE",
                    "version": "0x3"
                }
                """.trimIndent()
            }
        val mockedResponse =
            """
            {
                "id": 0,
                "jsonrpc": "2.0",
                "result": {
                    "block_hash": "0x1",
                    "block_number": 111,
                    "l1_da_mode": "BLOB",
                    "l1_data_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l1_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "l2_gas_price": {
                        "price_in_fri": "0x1",
                        "price_in_wei": "0x1"
                    },
                    "new_root": "0x1",
                    "parent_hash": "0x1",
                    "sequencer_address": "0x1",
                    "starknet_version": "0.14.0",
                    "event_commitment": "0x1",
                    "transaction_commitment": "0x1",
                    "receipt_commitment": "0x1",
                    "state_diff_commitment": "0x1",
                    "event_count": 1,
                    "transaction_count": ${tips.size},
                    "state_diff_length": 1,
                    "status": "ACCEPTED_ON_L2",
                    "timestamp": 123,
                    "transactions": [$transactions]
                }
            }
            """.trimIndent()

        val httpService =
            mock<HttpService> {
                on { send(any()) } doReturn HttpResponse(true, 200, mockedResponse)
            }
        val provider = JsonRpcProvider("", httpService)
        return estimateTip(provider).send()
    }
}
