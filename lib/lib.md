# Module lib

StarkNet.kt is a library allowing for easy interaction with StarkNet gateway and nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, StarkNet.kt has been created with compatibility with Java in mind.

## Example usages in Kotlin

```kotlin
fun main() {
    // Create a provider for interacting with StarkNet
    val provider = GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET)

    // Create a account interface
    val account = StandardAccount(provider, Felt(1234), Felt(1234))

    // Make a request
    val request = account.getStorageAt(Felt(1111), Felt(1234), BlockTag.LATEST)
    val response = request.send()
    response.result
}
```

## Example usages in Java

```java
public class StarknetExample {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        var provider = new GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET);
        
        // Create a account interface
        var account = new StandardAccount(provider, new Felt(1234), new Felt(1234));
        
        // Make a request
        var request = account.getStorageAt(new Felt(111), new Felt(123), BlockTag.LATEST);
        var response = request.send();
        response.getResult();
    }
}
```

## Asynchronous requests in Kotlin

It is also possible to make asynchronous requests. `Request.sendAsync()` returs a `CompletableFuture`
that can be than handled in preferred way.

```kotlin
fun main() {
    val provider = GatewayProvider("feeder_gateway_url", "gateway_url", StarknetChainId.TESTNET)
    val account = StandardAccount(provider = provider, address = Felt(1234), privateKey = Felt(1234))
    val request = account.getStorageAt(contractAddress = Felt(1111), key = Felt(1234), blockTag = BlockTag.LATEST)
    val future = request.sendAsync()
    val response = future.get()
    response.result
}
```

## Asynchronous requests in Java

```java
public class Main {
    public static void main(String[] args) {
        var provider = new GatewayProvider("", "", StarknetChainId.TESTNET);
        var account = new StandardAccount(provider, new Felt(1234), new Felt(1234));
        var request = account.getStorageAt(new Felt(111), new Felt(123), BlockTag.LATEST);
        var future = request.sendAsync();

        try {
            var response = future.get();
            var result = response.getResult();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

    }
}
```
