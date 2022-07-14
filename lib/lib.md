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
        Felt accountAddress = new Felt(45234235672347L);
        Felt privateKey = new Felt(5326326273453L);
        var account = new StandardAccount(provider, accountAddress, privateKey);
        
        // Make a request
        Felt contractAddress = new Felt(42362362436L);
        Felt storageKey = new Felt(13241253414L);
        Request<GetStorageAtResponse> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);

        GetStorageAtResponse response = request.send();
        response.getResult();
    }
}
```

## Example usages in Kotlin

```kotlin
fun main() {
    // Create a provider for interacting with StarkNet
    val provider = GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET)

    // Create an account interface
    val accountAddress = Felt(1052524524L)
    val privateKey = Felt(4232362662L)
    val account = StandardAccount(provider, accountAddress, privateKey)

    // Make a request
    val contractAddress = Felt(423623626L)
    val storageKey = Felt(132412414L)
    val request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val response = request.send()
    response.result
}
```

## Asynchronous requests in Java

```java
public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        var provider = new GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET);

        // Create an account interface
        Felt accountAddress = new Felt(45234235672347L);
        Felt privateKey = new Felt(5326326273453L);
        var account = new StandardAccount(provider, accountAddress, privateKey);

        // Make an asynchronous request
        Felt contractAddress = new Felt(42362362436L);
        Felt storageKey = new Felt(13241253414L);
        Request<GetStorageAtResponse> request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        CompletableFuture<GetStorageAtResponse> future = request.sendAsync();

        try {
            GetStorageAtResponse result = future.get();
            result.getResult();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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
    val accountAddress = Felt(1052524524L)
    val privateKey = Felt(4232362662L)
    val account = StandardAccount(provider, accountAddress, privateKey)

    // Make an asynchronous request
    val contractAddress = Felt(423623626L)
    val storageKey = Felt(132412414L)
    val request = account.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val future = request.sendAsync()

    val response = future.get()
    response.result
}
```
