# Java demo

## Table of contents

<!-- TOC -->
* [Java demo](#java-demo)
  * [Table of contents](#table-of-contents)
  * [Running (devnet)](#running-devnet)
    * [Prerequisites](#prerequisites)
    * [Steps](#steps)
  * [Running (networks)](#running-networks)
    * [Prerequisites](#prerequisites-1)
    * [Steps](#steps-1)
<!-- TOC -->

## Running (devnet)

### Prerequisites
- [starknet-devnet-rs](https://github.com/0xSpaceShard/starknet-devnet-rs)
- A tool for compiling Cairo 0 contracts and calculating their class hashes.
### Steps
1. Install `starknet-devnet-rs`. Since it has yet to be released, you will need to build it manually:
    ```shell
    git clone https://github.com/0xSpaceShard/starknet-devnet-rs.git starknet-devnet-rs
    cd starknet-devnet-rs
    cargo build --release
    export DEVNET_PATH=$(pwd)/target/release/starknet-devnet
    ```
2. Run devnet with specific parameters on your host machine:
    ```shell
    $DEVNET_PATH --host 127.0.0.1 --port 5050 --seed 1053545547
    ```
3. Compile a Cairo 0 balance contract and calculate its class hash.
A **path** to compiled contract and **class hash** should be set manually in [Main.java](src/main/java/com/example/javademo/Main.java)
4. Run the demo:
   ```shell
   ./gradlew :javademo:run
   ```

## Running (networks)
Running the demo on a network other than devnet (Mainnet/Testen/Integration) requires some tweaks to be made.
### Prerequisites
- URL of a Starknet RPC node.
- Account deployed on said network with some funds on it. If you're using **testnet**, you can obtain some funds from the [faucet](https://faucet.goerli.starknet.io/).
- A valid configuration that consists of the data above:
   - `DEMO_RPC_URL` - RPC node URL
   - `DEMO_ACCOUNT_ADDRESS` - account address
   - `DEMO_ACCOUNT_PRIVATE_KEY` - account private key
- A tool for compiling Cairo 0 contracts and calculating their class hashes.

### Steps
1. Set a config with your data. To do so, you can modify `DemoConfig` in [Main.java](src/main/java/com/example/javademo/Main.java):
2. Compile a Cairo 0 balance contract and calculate its class hash.
   A **path** to compiled contract and **class hash** should be set manually in [Main.java](src/main/java/com/example/javademo/Main.java)
3. Run the demo:
   ```shell
   ./gradlew :javademo:run
   ```
