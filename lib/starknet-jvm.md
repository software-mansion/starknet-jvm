# Module starknet-jvm

StarkNet-jvm is a library allowing for easy interaction with StarkNet gateway and nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, StarkNet-jvm has been created with compatibility with Java in mind.

## Making synchronous requests

### In Java

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

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.gateway.GatewayProvider

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

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.gateway.GatewayProvider

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

# Package com.swmansion.starknet.account

Account interface used to simplify preparing, signing StarkNet transactions and automatic fee estimation.

Example usages of `StandardAccount`

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.InvokeFunctionPayload;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Provider provider = GatewayProvider.makeTestnetClient();
        Felt address = new Felt(0x1234);
        Felt privateKey = new Felt(0x1);
        Account account = new StandardAccount(provider, address, privateKey);

        // Execute a single call
        Felt maxFee = new Felt(10000000L);
        Felt contractAddress = new Felt(0x1111);
        Call call = new Call(contractAddress, "increase_balance", List.of(new Felt(100)));
        Request<InvokeFunctionResponse> request = account.execute(call, maxFee);
        InvokeFunctionResponse response = request.send();

        // Execute multiple calls
        Call call1 = new Call(contractAddress, "increase_balance", List.of(new Felt(100)));
        Call call2 = new Call(contractAddress, "increase_balance", List.of(new Felt(200)));
        account.execute(List.of(call1, call2), maxFee).send();

        // Use automatic maxFee estimation
        account.execute(call).send();
        // or
        account.execute(List.of(call1, call2)).send();

        // Construct transaction step by step
        Call otherCall = new Call(contractAddress, "increase_balance", List.of(new Felt(100)));
        EstimateFeeResponse feeEstimate = account.estimateFee(otherCall).send();
        Felt nonce = account.getNonce().send();
        InvokeFunctionPayload signedTransaction = account.sign(otherCall, new ExecutionParams(nonce, maxFee));
        InvokeFunctionResponse signedInvokeResponse = provider.invokeFunction(signedTransaction).send();
    }
}
```

or in Kotlin

```kotlin
fun main(args: Array<String>) {
    val provider: Provider = makeTestnetClient()
    val address = Felt(0x1234)
    val privateKey = Felt(0x1)
    val account: Account = StandardAccount(provider, address, privateKey)

    // Execute a single call
    val maxFee = Felt(10000000L)
    val contractAddress = Felt(0x1111)
    val call = Call(contractAddress, "increase_balance", listOf(Felt(100)))
    val request = account.execute(call, maxFee)
    val response = request.send()

    // Execute multiple calls
    val call1 = Call(contractAddress, "increase_balance", listOf(Felt(100)))
    val call2 = Call(contractAddress, "increase_balance", listOf(Felt(200)))
    account.execute(listOf(call1, call2), maxFee).send()

    // Use automatic maxFee estimation
    account.execute(call).send()
    // or
    account.execute(listOf(call1, call2)).send()

    // Construct transaction step by step
    val otherCall = Call(contractAddress, "increase_balance", listOf(Felt(100)))
    val (gasConsumed, gasPrice, overallFee) = account.estimateFee(otherCall).send()
    val nonce = account.getNonce().send()
    val signedTransaction = account.sign(otherCall, ExecutionParams(nonce, maxFee))
    val signedInvokeResponse = provider.invokeFunction(signedTransaction).send()
}
```

# Package com.swmansion.starknet.crypto

Cryptography and signature related classes.

# Package com.swmansion.starknet.data

Data classes representing StarkNet objects and utilities for handling them.

# Package com.swmansion.starknet.data.types

Data classes representing StarkNet objects.

# Package com.swmansion.starknet.data.types.transactions

Data classes representing StarkNet transactions.

# Package com.swmansion.starknet.provider

Provider interface used for interacting with StarkNet.

# Package com.swmansion.starknet.provider.exceptions

Exceptions thrown by the StarkNet providers.

# Package com.swmansion.starknet.provider.gateway

Provider utilising StarkNet gateway and feeder gateway for communication with the network.

# Package com.swmansion.starknet.provider.rpc

Provider implementing the [JSON RPC interface](https://github.com/starkware-libs/starknet-specs)
to communicate with the network.

# Package com.swmansion.starknet.service.http

Http service used to communicate with StarkNet.

You can create a `OkHttpService` yourself and pass it whenever creating a provider. This way your whole
application can use a single `OkHttpClient`. Read more [here](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared).

```java
import com.swmansion.starknet.service.http.OkHttpService;

// (...)

var httpClient = new OkHttpClient();
var httpService = new OkHttpService(httpClient);

var provider = GatewayProvider.makeTestnetClient(httpService);
var account1 = new StandardAccount(provider, accountAddress1, privateKey1);
var account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```

# Package com.swmansion.starknet.signer

Signer interface used to sign StarkNet transactions.
