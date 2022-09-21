# Module starknet-jvm

StarkNet.kt is a library allowing for easy interaction with StarkNet gateway and nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, StarkNet.kt has been created with compatibility with Java in mind.

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

Account interface used to simplify preparing and signing StarkNet transactions.

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

// ...

var httpClient = new OkHttpClient();
var httpService = new OkHttpService(httpClient);

var provider = GatewayProvider.makeTestnetClient(httpService);
var account1 = new StandardAccount(provider, accountAddress1, privateKey1);
var account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```

# Package com.swmansion.starknet.signer

Signer interface used to sign StarkNet transactions.
