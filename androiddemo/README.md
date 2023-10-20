# Android demo

## Prerequisites
1. You will need to provide RPC node URL.
To do that, you can either set `RPC_URL` environment variable or modify [build.gradle.kts](build.gradle.kts):
    ```gradle
    android {
        ...
        defaultConfig {
            ...
            buildConfigField("String", "RPC_URL", "http://example-node-url.com/rpc")
            }
        }
    }
    ```
2.  To use all demo functionalities, ou will need a Starknet account (its address and private key) with some funds on it.