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
  * [Guides](#guides)
  * [Demo applications](#demo-applications)
    * [Android demo](#android-demo)
    * [Java demo](#java-demo)
  * [Development](#development)
    * [Hooks](#hooks)
  * [Running tests](#running-tests)
    * [Prerequisites](#prerequisites)
    * [Regular Tests](#regular-tests)
    * [Network Tests](#network-tests)
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
### Transferring STRK tokens
```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.data.types.Uint256
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    val provider = JsonRpcProvider("https://your.node.url")
    val account = StandardAccount(
        address = Felt.fromHex("0x123"),
        privateKey = Felt.fromHex("0x456"),
        provider = provider,
        chainId = StarknetChainId.SEPOLIA,
    )


    val amount = Uint256(Felt(100))
    val recipientAccountAddress = Felt.fromHex("0x789")
    val strkContractAddress = Felt.fromHex("0x04718f5a0fc34cc1af16a1cdee98ffb20c31f5cd61d6ab07201858f4287c938d")
    val call = Call(
        contractAddress = strkContractAddress,
        entrypoint = "transfer",
        calldata = listOf(recipientAccountAddress) + amount.toCalldata(),
    )

    val request = account.executeV3(call)
    val response = request.send()
}
```

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Provider provider = new JsonRpcProvider("https://your.node.url");

        Account account = new StandardAccount(
                Felt.fromHex("0x123"),
                Felt.fromHex("0x456"),
                provider,
                StarknetChainId.SEPOLIA
        );

        Uint256 amount = new Uint256(new Felt(100));
        Felt recipientAccountAddress = Felt.fromHex("0x789");
        Felt strkContractAddress = Felt.fromHex("0x04718f5a0fc34cc1af16a1cdee98ffb20c31f5cd61d6ab07201858f4287c938d");
        Call call = Call.fromCallArguments(
                strkContractAddress,
                "transfer",
                List.of(recipientAccountAddress, amount)
        );

        Request<InvokeFunctionResponse> executeRequest = account.executeV3(call);
        InvokeFunctionResponse executeResponse = executeRequest.send();
    }
}
```
For more example usages, see guides below👇

## Guides
- [Koltin guide](lib/kotlin-guide.md)
- [Java guide](lib/java-guide.md)


## Demo applications
These demo apps can be used with any Starknet RPC node, including devnet.
They are intended for demonstration/testing purposes only. 
### [Android demo](androiddemo)
### [Java demo](javademo)

## Development

### Hooks
Run
```
./gradlew installKotlinterPrePushHook
```

### Update submodules
Run
```
git submodule update --init --recursive
```

## Running tests

### Prerequisites
- [`starknet-devnet-rs`](https://github.com/0xSpaceShard/starknet-devnet-rs) 
  - Since it has yet to be released, you will need to build it manually and set `DEVNET_PATH` environment variable that points to a binary:
    ```shell
    DEVNET_PATH=/path/to/starknet-devnet-rs/target/release/starknet-devnet
    ```
  - You can do so by using environment variables in your system or IDE, or by sourcing an `.env` file. Refer to the example config found in [test_variables.env.example](test_variables.env.example).
- [`starknet-foundry`](https://github.com/foundry-rs/starknet-foundry) - provides `sncast` cli
- [`asdf`](https://github.com/asdf-vm/asdf) version manager and [`asdf scarb`](https://github.com/software-mansion/asdf-scarb) plugin
- [`java`](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html) - make sure to have Java 11 installed and set the `JAVA_HOME` environment variable to the path of your JDK installation
- [`cmake`](https://github.com/Kitware/CMake/releases/tag/v3.18.1) - make sure to have `cmake` 3.18.1  installed

### Regular Tests
Use the following command to run tests:
```shell
./gradlew :lib:test
```

### Network Tests
Running tests on networks requires a valid configuration. It can be set using environment variables in your system or IDE, or by sourcing an `.env` file. 
Refer to the example config found in [test_variables.env.example](test_variables.env.example).
To select the network, please set the `NETWORK_TEST_NETWORK_NAME` environment variable. Currenty, the allowed options are:
  - `SEPOLIA_TESTNET`
  - `SEPOLIA_INTEGRATION`

[comment]: <> (TODO: #384 Test v3 transactions on Sepolia)
Note: The transition of network tests from `GOERLI` to `SEPOLIA` networks results in a current limitation of v3 tests.
To properly configure your network, ensure the following variables are set with the `NETWORK_NAME_` prefix:  
  - `RPC_URL` - url of your RPC node
  - `ACCOUNT_ADDRESS` and `PRIVATE_KEY` - address and private key of your account

Additionally, you can also set:
  - `CONST_NONCE_ACCOUNT_ADDRESS` and `CONST_NONCE_PRIVATE_KEY` - address and private key exclusively for non-gas network tests, preventing potential inconsistencies (sometimes, `getNonce` may report higher nonce than expected).
  Recommended for reliable non-gas testing. 
  These default to `ACCOUNT_ADDRESS` and `PRIVATE_KEY` if not set.
  - `ACCOUNT_CAIRO_VERSION` - Cairo version of the `ACCOUNT_ADDRESS` and `CONST_NONCE_ACCOUNT_ADDRESS` accounts. Defaults to `0`.

Network tests are disabled by default. To enable them, you can set the environment variable: 
```env
NETWORK_TEST_MODE=non_gas
```
Some network tests require gas and are disabled by default. If you want to run them as well, you can set:
```env 
NETWORK_TEST_MODE=all
```
⚠️ WARNING ⚠️ Please be aware that in that case your account address must have a pre-existing balance as these tests will consume some funds.

Alternatively, you can use flag to specify whether to run network and gas tests:
```shell
./gradlew :lib:test -PnetworkTestMode=non_gas
./gradlew :lib:test -PnetworkTestMode=all
```
Flag takes precendece over the environment variable if both are set.

### Ensuring idiomatic Java code
We want this library to be used by both kotlin & java users. In order to ensure a nice API for java always follow those rules: 
1. When using file level functions use `@file:JvmName(NAME)` to ensure a nice name without `Kt` suffix.
2. When using a companion object mark every property/function with `@JvmStatic`. This way they are accessible as static from the class. Without it `Class.INSTANCE` would have to be used.
3. When defining an immutable constant use `@field:JvmField`. This makes them static properties without getters/setters in java.
4. If you are not sure how something would work in java just create a new java class, import your code and check yourself.
5. Avoid using default arguments. It is better to overload a function and specify defaults there.


## Building documentation

User guides for Kotlin and Java are generated automatically based on the [guide.md](lib/guide.md) file (⚠️ only this file should be modified ⚠️).
This file can include both code snippets and code section tags (which are used to embed code from specific functions).

Functions eligible for inclusion in the code section can be located in any file within the following directories:
- [`lib/src/test/kotlin`](lib/src/test/kotlin) for Kotlin
- [`javademo/src/main/java/com/example/javademo`](javademo/src/main/java/com/example/javademo) for Java

These elements will be automatically included in the respective guides, ensuring that the documentation is always up to date with the code in the repository.

Code section template:
`<!-- codeSection(path="path/to/file", function="functionName", language="language") -->`

Code section examples:
* Kotlin - `<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeployAccountV3Transaction", language="Kotlin") -->
  `
* Java - `<!-- codeSection(path="Main.java", function="signAndSendDeployAccountV3Transaction", language="Java") -->
  `

Content of embedded functions can be tailored using `docsStart` and `docsEnd` comments inside the function. Only the content between those comments will be included in the documentation.


Execute following command to generate the guides:

```sh
./gradlew generateGuides
```

Documentation is written in Kdoc format and markdown and is generated using `Dokka`. Execute
following commands from `/lib` to build docs.

* `./gradlew dokkaHtml` to build kotlin format docs
* `./gradlew dokkaHtmlJava` to build java format docs

Generated documentation can be found in their respective folders inside `/build/dokka`.

## Release checklist
Perform these actions before releasing a new starknet-jvm version:
1. Checkout to `main` and pull
```
git checkout main && git pull
```
2. Create new branch for version bump
```
git checkout -b chore/bump-version-to-0.x.x
```
3. Update the version in `lib/build.gradle.kts` (following [semantic versioning](https://semver.org/)).
4. After merging PR, create a new tag
```
git checkout main && git pull

git tag -a 0.x.x -m "Version 0.x.x"
```
5. Push the tag (release workflow will be automatically triggered)
```
git push origin 0.x.x
```
