# Module starknet-jvm

Starknet-jvm is a library allowing for easy interaction with the Starknet JSON-RPC nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, Starknet-jvm has been created with compatibility with Java in mind.

## Reusing http clients

Make sure you don't create a new provider every time you want to use one. Instead, you should reuse existing instance.
This way you reuse connections and thread pools.

✅ **Do:**
```java
Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");
Account account1 = new StandardAccount(provider, accountAddress1, privateKey1);
Account account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```
```kotlin
val provider = JsonRpcProvider("https://example-node-url.com/rpc")
val account1 = StandardAccount(provider, accountAddress1, privateKey1)
val account2 = StandardAccount(provider, accountAddress2, privateKey2)
```

❌ **Don't:**
```java
Provider provider1 = new JsonRpcProvider("https://example-node-url.com/rpc");
Account account1 = new StandardAccount(provider1, accountAddress1, privateKey1);
Provider provider2 = new JsonRpcProvider("https://example-node-url.com/rpc");
Account account2 = new StandardAccount(provider2, accountAddress2, privateKey2);
```
```kotlin
val provider1 = JsonRpcProvider("https://example-node-url.com/rpc")
val account1 = StandardAccount(provider1, accountAddress1, privateKey1)
val provider2 = JsonRpcProvider("https://example-node-url.com/rpc")
val account2 = StandardAccount(provider2, accountAddress2, privateKey2)
```

## Making synchronous requests

### In Java

```java
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        Felt response = request.send();

        System.out.println(response);
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    // Make a request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val response = request.send()

    println(response)
}
```

## Making asynchronous requests

It is also possible to make asynchronous requests. `Request.sendAsync()` returns a `CompletableFuture`
that can be than handled in preferred way.

### In Java

```java
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        CompletableFuture<Felt> response = request.sendAsync();

        response.thenAccept(System.out::println);
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    // Make an asynchronous request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val future = request.sendAsync()

    future.thenAccept { println(it) }
}
```


## Deploying account V3
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeployAccountV3Transaction", language="Kotlin") -->

## Estimating fee for deploy account V3 transaction

<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeployAccountV3Transaction", language="Kotlin") -->

## Deploying account V1
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeployAccountV1Transaction", language="Kotlin") -->

## Estimating fee for deploy account V1 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeployAccountV1Transaction", language="Kotlin") -->

## Invoking contract: Transferring ETH
<!-- codeSection(path="network/account/AccountTest.kt", function="transferETH", language="Kotlin") -->

## Estimating fee for invoke V3 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForInvokeV3Transaction", language="Kotlin") -->

## Calling contract: Fetching ETH balance
<!-- codeSection(path="network/account/AccountTest.kt", function="getETHBalance", language="Kotlin") -->

## Making multiple calls: get multiple transactions data
<!-- codeSection(path="starknet/provider/ProviderTest.kt", function="batchGetTransactions", language="Kotlin") -->

## Making multiple calls of different types in one request
<!-- codeSection(path="starknet/provider/ProviderTest.kt", function="batchRequestsAny", language="Kotlin") -->

## Declaring Cairo 1/2 contract V3
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeclareV3Transaction", language="Kotlin") -->

## Estimating fee for declare V3 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeclareV3Transaction", language="Kotlin") -->

## Declaring Cairo 1/2 contract V2
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeclareV2Transaction", language="Kotlin") -->

## Estimating fee for declare V2 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeclareV2Transaction", language="Kotlin") -->

# Package com.swmansion.starknet.account
Account interface used to simplify preparing, signing Starknet transactions and automatic fee estimation.


## Example usage of `StandardAccount`
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="exampleAccountUsage", language="Kotlin") -->

# Package com.swmansion.starknet.crypto

Cryptography and signature related classes.
This is a low level module. Recommended way of using is through
Signer and Account implementations.

# Package com.swmansion.starknet.data
Data classes representing Starknet objects and utilities for handling them.

# Package com.swmansion.starknet.data.types
Data classes representing Starknet objects.

# Package com.swmansion.starknet.data.types.transactions
Data classes representing Starknet transactions.

# Package com.swmansion.starknet.deployercontract
Classes for interacting with Universal Deployer Contract (UDC).

<!-- codeSection(path="starknet/deployercontract/DeployerContractTest.kt", function="testUdcDeployV3", language="Kotlin") -->

# Package com.swmansion.starknet.provider
Provider interface and its implementations.

# Package com.swmansion.starknet.provider.exceptions

Exceptions thrown by the Starknet providers.

`Request.send()` throws `RequestFailedException` unchecked exception.
It can optionally be handled.
In the case of `Request.sendAsync()`, an exception would have to be handled in the returned `CompletableFuture`.

# Package com.swmansion.starknet.provider.rpc

Provider implementing the [JSON RPC interface](https://github.com/starkware-libs/starknet-specs)
to communicate with the network.

<!-- codeSection(path="starknet/provider/ProviderTest.kt", function="jsonRpcProviderCreationExample", language="Kotlin") -->

# Package com.swmansion.starknet.service.http

Http service used to communicate with Starknet.

You can create a `OkHttpService` yourself and pass it whenever creating a provider. This way your whole
application can use a single `OkHttpClient`. Read more [here](https://square.github.io/okhttp/).

```java
import com.swmansion.starknet.service.http.OkHttpService;

// (...)

OkHttpClient httpClient = new OkHttpClient();
OkHttpService httpService = new OkHttpService(httpClient);
```

# Package com.swmansion.starknet.signer

Signer interface and its implementations.
Recommended way of using Signer is through an Account.

```java
// Create a signer
Signer signer = ...
        
// Sign a transaction
List<Felt> signature = signer.signTransaction(tx);

// Get a public key
Felt publicKey = signer.getPublicKey();
```

