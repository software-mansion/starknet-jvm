# Module lib

StarkNet.kt is a library allowing for easy interaction with StarkNet gateway and nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, StarkNet.kt has been created with compatibility with Java in mind.

## Making synchronous requests

### In Java

```java
import starknet.account.StandardAccount;
import starknet.data.types.BlockTag;
import starknet.data.types.Felt;
import starknet.provider.Request;
import starknet.provider.gateway.GatewayProvider;


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

### In Kotlin

```kotlin
import starknet.account.StandardAccount
import starknet.data.types.BlockTag
import starknet.data.types.Felt
import starknet.provider.gateway.GatewayProvider

fun main() {
    // Create a provider for interacting with StarkNet
    val provider = GatewayProvider.makeTestnetClient()

    // Create an account interface
    val accountAddress = Felt.fromHex("0x1052524524")
    val privateKey = Felt.fromHex("0x4232362662")
    val account = StandardAccount(provider, accountAddress, privateKey)

    // Make a request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val response = request.send()

    println(response)
}
```

## Making asynchronous requests

It is also possible to make asynchronous requests. `Request.sendAsync()` returs a `CompletableFuture`
that can be than handled in preferred way.

### In Java

```java
import starknet.account.StandardAccount;
import starknet.data.types.BlockTag;
import starknet.data.types.Felt;
import starknet.provider.Request;
import starknet.provider.gateway.GatewayProvider;

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

### In Kotlin

```kotlin
import starknet.account.StandardAccount
import starknet.data.types.BlockTag
import starknet.data.types.Felt
import starknet.provider.gateway.GatewayProvider

fun main() {
    // Create a provider for interacting with StarkNet
    val provider = GatewayProvider.makeTestnetClient()

    // Create an account interface
    val accountAddress = Felt.fromHex("0x1052524524")
    val privateKey = Felt.fromHex("0x4232362662")
    val account = StandardAccount(provider, accountAddress, privateKey)

    // Make an asynchronous request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val future = request.sendAsync()

    future.thenAccept { println(it) }
}
```
