# Module lib

StarkNet.kt is a library allowing for easy interaction with StarkNet gateway and nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, StarkNet.kt has been created with compatibility with Java in mind.

## Example usages in Java

```java
public class StarknetExample {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        var provider = new GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET);

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        var account = new StandardAccount(provider, accountAddress, privateKey);

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<GetStorageAtResponse> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        GetStorageAtResponse response = request.send();

        System.out.println(response.getResult());
    }
}
```

## Example usages in Kotlin

```kotlin
fun main() {
    // Create a provider for interacting with StarkNet
    val provider = GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET)

    // Create an account interface
    val accountAddress = Felt.fromHex("0x1052524524")
    val privateKey = Felt.fromHex("0x4232362662")
    val account = StandardAccount(provider, accountAddress, privateKey)

    // Make a request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val response = request.send()

    println(response.result)
}
```

## Asynchronous requests in Java

```java
public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        var provider = new GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET);

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        var account = new StandardAccount(provider, accountAddress, privateKey);

        // Make an asynchronous request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<GetStorageAtResponse> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        CompletableFuture<GetStorageAtResponse> future = request.sendAsync();

        future.thenAccept(res -> System.out.println(res.getResult()));
    }
}
```

## Asynchronous requests in Kotlin

It is also possible to make asynchronous requests. `Request.sendAsync()` returs a `CompletableFuture`
that can be than handled in preferred way.

```kotlin
fun main() {
    // Create a provider for interacting with StarkNet
    val provider = GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET)

    // Create an account interface
    val accountAddress = Felt.fromHex("0x1052524524")
    val privateKey = Felt.fromHex("0x4232362662")
    val account = StandardAccount(provider, accountAddress, privateKey)

    // Make an asynchronous request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val future = request.sendAsync()

    future.thenAccept {
        println(it.result)
    }
}
```
