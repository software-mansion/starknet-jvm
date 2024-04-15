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
    * [Note](#note)
<!-- TOC -->

## Running (devnet)

### Prerequisites
- [`starknet-devnet-rs`](https://github.com/0xSpaceShard/starknet-devnet-rs)
- [`asdf`](https://github.com/asdf-vm/asdf) version manager with [`asdf scarb`](https://github.com/software-mansion/asdf-scarb) plugin

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
3. Compile a [Cairo 1 demo contract](src/main/resources/contracts). Run:
    ```shell
    cd src/main/resources/contracts
    scarb --release build
    ```
4. Run the demo:
   ```shell
   ./gradlew :javademo:run
   ```

## Running (networks)
Running the demo on a network other than devnet (Mainnet/Testen/Integration) requires some tweaks to be made.

### Prerequisites
- URL of a Starknet RPC node.
- Details (address and private key) of an account deployed on said network with some funds on it.
- [`asdf`](https://github.com/asdf-vm/asdf) version manager with [`asdf scarb`](https://github.com/software-mansion/asdf-scarb) plugin

### Steps
1. Set a config with your data. 
To do so, you can modify `DemoConfig` in [Main.java](src/main/java/com/example/javademo/Main.java).
Make sure to change profile to `NETWORK` and set the correct values for:
   - RPC node URL
   - account address
   - account private key
2. Make sure to slightly modify the [demo contract source code](src/main/resources/contracts/src/balance.cairo) (e.g. add a salt to the function names).
Duplicate contracts are not allowed on the same network.
3. Repeat steps 3-4 from [Running (devnet)](#running-devnet).

### Note
The time it takes for a transaction to be processed on a network other than devnet can vary.
If the demo fails because transaction was not processed in time, try increasing the timeouts in [Main.java](src/main/java/com/example/javademo/Main.java).