# Module starknet-jvm

Starknet-jvm is a library allowing for easy interaction with the Starknet JSON-RPC nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, Starknet-jvm has been created with compatibility with Java in mind.

## Quickstart
### Using provider
`Provider` is a facade for interacting with Starknet. `JsonRpcProvider` is a client which interacts with a Starknet full nodes like [Pathfinder](https://github.com/eqlabs/pathfinder), [Papyrus](https://github.com/starkware-libs/papyrus) or [Juno](https://github.com/NethermindEth/juno).
It supports read and write operations, like querying the blockchain state or adding new transactions.

```java
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

public class Main {
    public static void main(String[] args) {
        JsonRpcProvider provider = new JsonRpcProvider(DemoConfig.rpcNodeUrl);
        
        Request<BlockWithTxs> request = provider.getBlockWithTxs(1);
        BlockWithTxs response = request.send();
    }
}
```

### Reusing providers
Make sure you don't create a new provider every time you want to use one. Instead, you should reuse existing instance.
This way you reuse connections and thread pools.

✅ **Do:**
```java
Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");
Account account1 = new StandardAccount(provider, accountAddress1, privateKey1);
Account account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```


❌ **Don't:**
```java
Provider provider1 = new JsonRpcProvider("https://example-node-url.com/rpc");
Account account1 = new StandardAccount(provider1, accountAddress1, privateKey1);
Provider provider2 = new JsonRpcProvider("https://example-node-url.com/rpc");
Account account2 = new StandardAccount(provider2, accountAddress2, privateKey2);
```


### Creating account
`StandardAccount` is the default implementation of `Account` interface. It supports an account contract which proxies the calls to other contracts on Starknet.

Account can be created in two ways:

- By constructor (It is required to provide an address and either private key or signer).

- By methods `Account.signDeployAccountV3()` or `Account.signDeployAccountV3()`

There are some examples how to do it:



```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.data.types.StarknetChainId;

public class Main {
    public static void main(String[] args) {
        // If you don't have a private key, you can generate a random one
        Felt randomPrivateKey = StandardAccount.generatePrivateKey();

        // Create an instance of account which is already deployed
        // providing an address and a private key
        Account account = StandardAccount(
                Felt.fromHex("0x123"),
                Felt.fromHex("0x456"),
                provider,
                StarknetChainId.SEPOLIA
                );

        // It's possible to specify a signer
        Account accountWithSigner = StandardAccount(
                Felt.fromHex("0x123"),
                signer,
                provider,
                StarknetChainId.SEPOLIA
        );
    }
}
```

### Using account - transferring STRK tokens


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

## Making synchronous requests

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



## Making asynchronous requests

It is also possible to make asynchronous requests. `Request.sendAsync()` returns a `CompletableFuture`
that can be than handled in preferred way.

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




## Deploying account V3



## Estimating fee for deploy account V3 transaction



## Deploying account V1


## Estimating fee for deploy account V1 transaction


## Invoking contract: Transferring ETH


## Estimating fee for invoke V3 transaction


## Calling contract: Fetching ETH balance


## Making multiple calls: get multiple transactions data


## Making multiple calls of different types in one request


## Declaring Cairo 1/2 contract V3


## Estimating fee for declare V3 transaction


## Declaring Cairo 1/2 contract V2


## Estimating fee for declare V2 transaction


# Package com.swmansion.starknet.account
Account interface used to simplify preparing, signing Starknet transactions and automatic fee estimation.


## Example usage of `StandardAccount`


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

