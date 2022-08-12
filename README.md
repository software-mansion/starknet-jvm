<h2 align="center">☕ starknet jvm ☕</h2>

[StarkNet](https://starkware.co/starknet/#:~:text=Live%20on%20Mainnet-,What%20is%20StarkNet%3F,-StarkNet%20is%20a) SDK for JVM languages:
- Java
- Kotlin
- Scala
- Clojure
- Groovy

## Table of contents

<!-- TOC -->
  * [Documentation](#documentation)
  * [Example usages](#example-usages)
    * [Making synchronous requests](#making-synchronous-requests)
    * [Making asynchronous requests](#making-asynchronous-requests)
  * [Development](#development)
    * [Hooks](#hooks)
    * [Ensuring idiomatic Java code](#ensuring-idiomatic-java-code)
  * [Building documentation](#building-documentation)
<!-- TOC -->

## Documentation

Documentation is provided in two formats:

- [Java and other jvm languages](https://docs.swmansion.com/starknet-jvm/)
- [Kotlin](https://docs.swmansion.com/starknet-jvm/kotlin/)


## Example usages

### Making synchronous requests

```java
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        var provider = GatewayProvider.makeTestnetClient();

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        var account = new StandardAccount(provider, accountAddress, privateKey);

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
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        var provider = GatewayProvider.makeTestnetClient();

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        var account = new StandardAccount(provider, accountAddress, privateKey);

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        CompletableFuture<Felt> response = request.sendAsync();

        response.thenAccept(System.out::println);
    }
}
```

## Development

### Hooks
Run
```
./gradlew addKtlintFormatGitPreCommitHook
./gradlew addKtlintCheckGitPreCommitHook
```


### Ensuring idiomatic Java code
We want this library to be used by both kotlin & java users. In order to ensure a nice API for java always follow those rules: 
1. When using file level functions use `@file:JvmName(NAME)` to ensure a nice name without `Kt` suffix.
2. When using a companion object mark every property/function with `@JvmStatic`. This way they are accessible as static from the class. Without it `Class.INSTANCE` would have to be used.
3. When defining an immutable constant use `@field:JvmField`. This makes them static properties without getters/setters in java.
4. If you are not sure how something would work in java just create a new java class, import your code and check yourself.


## Building documentation

Documentation is written in Kdoc format and markdown and is generated using `Dokka`. Execute
following commands from `/lib` to build docs.

* `./gradlew dokkaHtml` to build kotlin format docs
* `./gradlew dokkaHtmlJava` to build java format docs

Generated documentation can be found in their respective folders inside `/build/dokka`.
