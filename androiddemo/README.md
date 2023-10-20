# Android demo

## Prerequisites
Running the demo requires a valid configuration. It can be set using environment variables in your system or IDE, or by sourcing an `.env` file.
Refer to the example config found in [test_variables.env.example](../test_variables.env.example).
Out of the variables listed there, the following are required:
- `DEMO_RPC_URL` - RPC node URL
- `DEMO_ACCOUNT_ADDRESS` - account address
- `DEMO_ACCOUNT_PRIVATE_KEY` - account private key

Alternatevily, you can modify [build.gradle.kts](build.gradle.kts) to set those variables:
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
To use all demo functionalities, your Starknet account should have some funds on it.