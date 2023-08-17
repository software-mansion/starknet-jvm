<h2 align="center">☕ starknet jvm ☕</h2>

[StarkNet](https://starkware.co/starknet/#:~:text=Live%20on%20Mainnet-,What%20is%20StarkNet%3F,-StarkNet%20is%20a) SDK for JVM languages:
- Java
- Kotlin
- Scala
- Clojure
- Groovy

## Table of contents

<!-- TOC -->
  * [Installation](#installation)
  * [Documentation](#documentation)
  * [Example usages](#example-usages)
    * [Making synchronous requests](#making-synchronous-requests)
    * [Making asynchronous requests](#making-asynchronous-requests)
  * [Development](#development)
    * [Hooks](#hooks)
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
        // Create a provider for interacting with StarkNet
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
        // Create a provider for interacting with StarkNet
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

### Deploy Account example

```java
import org.junit.jupiter.api.Test;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.ContractAddressCalculator;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.*;
import com.swmansion.starknet.data.types.transactions.TransactionReceipt;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.math.BigInteger;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        JsonRpcProvider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

        // Create an account interface
        Felt privateKey = Felt.fromHex("0x12345");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);

        // Use the class hash of the desired account contract (i.e. the class hash of OpenZeppelin account contract)
        Felt classHash = Felt.fromHex("0x058d97f7d76e78f44905cc30cb65b91ea49a4b908a76703c54197bca90f81773");
        Felt salt = new Felt(789);
        List<Felt> calldata = List.of(publicKey);
        Felt address = ContractAddressCalculator.calculateAddressFromHash(
                classHash,
                calldata,
                salt
        );

        StandardAccount account = new StandardAccount(address, privateKey, provider, Felt.ZERO);
        
        Felt maxFee = Felt.fromHex("0x11fcc58c7f7000");  // should be 10*fee from estimate deploy account fee

        // Make sure to prefund the address with at least maxFee
        
        // Create and sign deploy account transaction
        DeployAccountTransactionPayload payload = account.signDeployAccount(
                classHash,
                calldata,
                salt,
                maxFee
        );

        DeployAccountResponse response = provider.deployAccount(payload).send();
    }
}
```

or in Kotlin

```kotlin
fun main(args: Array<String>) {
    // Create a provider for interacting with StarkNet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

    // Create an account interface
    val privateKey = Felt.fromHex("0x12345")
    val publicKey = StarknetCurve.getPublicKey(privateKey)

    // Use the class hash of desired account contract (i.e. the class hash of OpenZeppelin account contract)
    val classHash = Felt.fromHex("0x058d97f7d76e78f44905cc30cb65b91ea49a4b908a76703c54197bca90f81773")
    val salt = Felt(789)
    val calldata = listOf(publicKey)
    val address = ContractAddressCalculator.calculateAddressFromHash(
            classHash = classHash,
            calldata = calldata,
            salt = salt,
    )

    val account = StandardAccount(
            address,
            privateKey,
            provider,
    )

    val payload = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.fromHex("0x11fcc58c7f7000"),  // should be 10*fee from estimate deploy account fee
    )
    
    // Create and sign deploy account transaction
    val response = provider.deployAccount(payload).send()
}
```


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
Running tests requires to have both `cairo-lang` and `starknet-devnet` installed.
These are distributed as python packages. To install required dependencies, run:

```shell
pip install -r requirements.txt
```
### Regular Tests
Use the following command to run tests:
```shell
./gradlew :lib:test
```

### Integration Tests
Running tests for integration network requires a valid configuration. It can be set using environmental variables in your system or IDE, or by sourcing an `.env` file. 
Refer to the example config found in [integration_tests.env.example](integration_tests.env.example).
Please note that while there are publicly accessible gateway URLs, you will additionally need a `RPC node URL` and an `account address` (along with its `private key`), to run these tests.

Integration tests are disabled by default. To enable them, you can set the env variable: 
```env
ENABLE_INTEGRATION_TESTS=true
```

### Gas-requiring integration tests
Some tests require gas and are disabled by default. If you want to run them as well, you can set:
```env 
ENABLE_GAS_TESTS=true
```

Alternatively, you can use flags to specify whether to run integration and gas tests:
```shell
./gradlew :lib:test -PenableIntegrationTests=true
./gradlew :lib:test -PenableIntegrationTests=true -PenableGasTests=true
```
Please note that to run the gas-requiring tests both flags must be set to true.

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
