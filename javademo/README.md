# Java demo

## Prerequisites
1. Running the demo requires a valid configuration. It can be set using environment variables in your system or IDE, or by sourcing an `.env` file. Refer to the example config found in [test_variables.env.example](../test_variables.env.example). Out of the variables listed there, the following are required:
    - `STARKNET_RPC_URL` - RPC node URL
    - `STARKNET_ACCOUNT_ADDRESS` - account address
    - `STARKNET_ACCOUNT_PRIVATE_KEY` - account private key
2. Additionaly, you will need to compile a Cairo 0 balance contract and calculate its class hash.
The path to compiled contract and class hash should be set manually in [Main.java](src/main/java/com/example/javademo/Main.java)

To use all demo functionalities, your Starknet account should have some funds on it.