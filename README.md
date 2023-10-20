<h2 align="center">☕ starknet jvm ☕</h2>

[Starknet](https://starkware.co/starknet/#:~:text=Live%20on%20Mainnet-,What%20is%20Starknet%3F,-Starknet%20is%20a) SDK for JVM languages:
- Java
- Kotlin
- Scala
- Clojure
- Groovy

## Table of contents

<!-- TOC -->
  * [Table of contents](#table-of-contents)
  * [Installation](#installation)
  * [Documentation](#documentation)
  * [Example usages](#example-usages)
    * [Making synchronous requests](#making-synchronous-requests)
    * [Making asynchronous requests](#making-asynchronous-requests)
    * [Standard flow exampels](#standard-flow-exampels)
  * [Demo applications](#demo-applications)
    * [Android demo](#android-demo)
    * [Java demo](#java-demo)
  * [Reusing http clients](#reusing-http-clients)
  * [Development](#development)
    * [Hooks](#hooks)
  * [Running tests](#running-tests)
    * [Prerequisites](#prerequisites)
    * [Platform-specific prerequisites](#platform-specific-prerequisites)
    * [Regular Tests](#regular-tests)
    * [Integration Tests](#integration-tests)
    * [Ensuring idiomatic Java code](#ensuring-idiomatic-java-code)
  * [Building documentation](#building-documentation)
<!-- TOC -->

## Installation

Select the latest version from [the list](https://search.maven.org/artifact/com.swmansion.starknet/starknet) and follow installation instructions.

## Documentation

Documentation is provided in two formats:

- [Java and other jvm languages](https://docs.swmansion.com/starknet-jvm/)
- [Kotlin](https://docs.swmansion.com/starknet-jvm/kotlin/)


## Example usages

### Making synchronous requests

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = GatewayProvider.makeTestnetClient();

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        Account account = new StandardAccount(provider, accountAddress, privateKey);

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        Felt response = request.send();

        System.out.println(response);
    }
}
```

### Making asynchronous requests

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = GatewayProvider.makeTestnetClient();

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        Account account = new StandardAccount(provider, accountAddress, privateKey);

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        CompletableFuture<Felt> response = request.sendAsync();

        response.thenAccept(System.out::println);
    }
}
```

### Standard flow exampels

- [Deploying account](lib/starknet-jvm.md#deploying-account)
- [Invoking contract: Transfering ETH](lib/starknet-jvm.md#invoking-contract-transfering-eth)
- [Calling contract: Fetching ETH balance](lib/starknet-jvm.md#calling-contract-fetching-eth-balance)
- [Declaring Cairo 0 contract](lib/starknet-jvm.md#declaring-cairo-0-contract)
- [Declaring Cairo 1/2 contract](lib/starknet-jvm.md#declaring-cairo-12-contract)

## Demo applications
### [Android demo](androiddemo)
### [Java demo](javademo)


## Reusing http clients

Make sure you don't create a new provider every time you want to use one. Instead, you should reuse existing instance.
This way you reuse connections and thread pools.

✅ **Do:** 
```java
var provider = GatewayProvider.makeTestnetClient();
var account1 = new StandardAccount(provider, accountAddress1, privateKey1);
var account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```

❌ **Don't:**
```java
var provider1 = GatewayProvider.makeTestnetClient();
var account1 = new StandardAccount(provider1, accountAddress1, privateKey1);
var provider2 = GatewayProvider.makeTestnetClient();
var account2 = new StandardAccount(provider2, accountAddress2, privateKey2);
```


## Development

### Hooks
Run
```
./gradlew installKotlinterPrePushHook
```


## Running tests

### Prerequisites
- `cairo-lang` and `starknet-devnet`
  - These are distributed as python packages. To install, run:
    ```shell
    pip install -r requirements.txt
    ```
- [`starknet-devnet-rs`](https://github.com/0xSpaceShard/starknet-devnet-rs) 
  - Since it has yet to be released, you will need to build it manually and set `DEVNET_PATH` environment variable that points to a binary:
    ```shell
    DEVNET_PATH=/path/to/starknet-devnet-rs/target/release/starknet-devnet
    ```
  - You can do so by using environment variables in your system or IDE, or by sourcing an `.env` file. Refer to the example config found in [test_variables.env.example](test_variables.env.example).
- [`starknet-foundry`](https://github.com/foundry-rs/starknet-foundry) - provides `sncast` cli
- [`asdf`](https://github.com/asdf-vm/asdf) version manager and [`asdf scarb`](https://github.com/software-mansion/asdf-scarb) plugin

### Platform-specific prerequisites
- **macOS aarch64**: no additional steps are required.
- **linux x86_64**: no additional steps are required.
- For other platforms, you will need to set `V2_COMPILER_BUILD_PATH` environment variable.
    - `V2_COMPILER_BUILD_PATH` - path to a directory that contains built cairo v2.2.0 compiler binaries (`bin/`) and corelib (`corelib/`)
    - To build cairo compilers for your platform, refer to [cairo repo](https://github.com/starkware-libs/cairo).

### Regular Tests
Use the following command to run tests:
```shell
./gradlew :lib:test
```

### Integration Tests
Running tests for integration network requires a valid configuration. It can be set using environment variables in your system or IDE, or by sourcing an `.env` file. 
Refer to the example config found in [test_variables.env.example](test_variables.env.example).
Please note that while there are publicly accessible gateway URLs, you will additionally need a `RPC node URL` and an `account address` (along with its `private key`), to run these tests.

Integration tests are disabled by default. To enable them, you can set the env variable: 
```env
INTEGRATION_TEST_MODE=non_gas
```
Some of integration tests require gas and are disabled by default. If you want to run them as well, you can set:
```env 
INTEGRATION_TEST_MODE=all
```
⚠️ WARNING ⚠️ Please be aware that in that case your integration account address must have a pre-existing balance as these tests will consume some funds.

Alternatively, you can use flag to specify whether to run integration and gas tests:
```shell
./gradlew :lib:test -PintegrationTestMode=non_gas
./gradlew :lib:test -PintegrationTestMode=all
```
Flag takes precendece over the env variable if both are set.

⚠️ WARNING ⚠️ Some integration tests may fail due to getNonce receiving higher nonce than expected by other methods.
It is adviced to additionaly provide an account (along with its `private key`) with a constant `nonce` to ensure non-gas tests pass.
Such account shouldn't be used for any other purpose than running non-gas integration tests.
If not set, the main integration account will be used for this purpose.

### Ensuring idiomatic Java code
We want this library to be used by both kotlin & java users. In order to ensure a nice API for java always follow those rules: 
1. When using file level functions use `@file:JvmName(NAME)` to ensure a nice name without `Kt` suffix.
2. When using a companion object mark every property/function with `@JvmStatic`. This way they are accessible as static from the class. Without it `Class.INSTANCE` would have to be used.
3. When defining an immutable constant use `@field:JvmField`. This makes them static properties without getters/setters in java.
4. If you are not sure how something would work in java just create a new java class, import your code and check yourself.
5. Avoid using default arguments. It is better to overload a function and specify defaults there.


## Building documentation

Documentation is written in Kdoc format and markdown and is generated using `Dokka`. Execute
following commands from `/lib` to build docs.

* `./gradlew dokkaHtml` to build kotlin format docs
* `./gradlew dokkaHtmlJava` to build java format docs

Generated documentation can be found in their respective folders inside `/build/dokka`.
