# Module starknet-jvm

StarkNet-jvm is a library allowing for easy interaction with StarkNet gateway and nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, StarkNet-jvm has been created with compatibility with Java in mind.

⚠️Gateway provider is currently marked as deprecated and will soon be removed. Please use JSON-RPC provider instead.

## Making synchronous requests

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

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
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with StarkNet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

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

It is also possible to make asynchronous requests. `Request.sendAsync()` returns a `CompletableFuture`
that can be than handled in preferred way.

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.BlockTag;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with StarkNet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

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
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with StarkNet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)
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

### Example usages of `StandardAccount`

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.InvokeFunctionPayload;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);
        Felt address = new Felt(0x1234);
        Felt privateKey = new Felt(0x1);
        Account account = new StandardAccount(address, privateKey, provider);

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
        InvokeTransactionPayload signedTransaction = account.sign(otherCall, new ExecutionParams(nonce, maxFee));
        InvokeFunctionResponse signedInvokeResponse = provider.invokeFunction(signedTransaction).send();

        // Sign transaction for fee estimation only
        InvokeTransactionPayload transactionForFeeEstimation = account.sign(call, new ExecutionParams(nonce, Felt.ZERO), true);

        // Sign and verify TypedData signature
        TypedData typedData = TypedData.fromJsonString("...");
        List<Felt> typedDataSignature = account.signTypedData(typedData);
        Request<Boolean> isValidSignatureRequest = account.verifyTypedDataSignature(typedData, typedDataSignature);
        boolean isValidSignature = isValidSignatureRequest.send();
    }
}
```

or in Kotlin

```kotlin
fun main(args: Array<String>) {
    val provider: Provider = makeTestnetProvider()
    val address = Felt(0x1234)
    val privateKey = Felt(0x1)
    val account: Account = StandardAccount(address, privateKey, provider)

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
    
    // Sign transaction for fee estimation only
    val transactionForFeeEstimation = account.sign(call, ExecutionParams(nonce, Felt.ZERO), true)

    // Sign and verify TypedData signature
    val typedData = TypedData.fromJsonString("...");
    val typedDataSignature = account.signTypedData(typedData)
    val isValidSignatureRequest = account.verifyTypedDataSignature(typedData, typedDataSignature)
    val isValidSignature = isValidSignatureRequest.send()
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

        // Estimate the fee for deploying the account
        DeployAccountTransactionPayload payloadForFeeEstimation = account.signDeployAccount(
                classHash,
                calldata,
                salt,
                Felt.ZERO,
                Felt.ZERO,
                true
        );

        List<EstimateFeeResponse> feePayload = provider.getEstimateFee(
                List.of(payloadForFeeEstimation)
        ).send();
        Felt estimateDeployAccountFee = feePayload.get(0).getOverallFee();
        // Multiply the estimated fee by 10 to ensure the transaction will not fail due to insufficient funds
        Felt maxFee = new Felt(estimateDeployAccountFee.getValue().multiply(BigInteger.TEN));

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

    // Estimate the fee for deploying the account
    val payloadForFeeEstimation = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = Felt.ZERO,
            nonce = Felt.ZERO,
            forFeeEstimate = true,
    )
    val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
    val fee = feePayload.first().overallFee
    // Multiply the estimated fee by 10 to ensure the transaction will not fail due to insufficient funds
    val maxFee = Felt(fee.value.multiply(BigInteger.TEN))
    
    //  Make sure to prefund the address with at least maxFee

    val payload = account.signDeployAccount(
            classHash = classHash,
            salt = salt,
            calldata = calldata,
            maxFee = maxFee,
    )
    
    // Create and sign deploy account transaction
    val response = provider.deployAccount(payload).send()
}
```

# Package com.swmansion.starknet.crypto

Cryptography and signature related classes.
This is a low level module. Recommended way of using is through
Signer and Account implementations.

# Package com.swmansion.starknet.data

Data classes representing StarkNet objects and utilities for handling them.

# Package com.swmansion.starknet.data.types

Data classes representing StarkNet objects.

# Package com.swmansion.starknet.data.types.transactions

Data classes representing StarkNet transactions.

# Package com.swmansion.starknet.deployercontract

Classes for interacting with Universal Deployer Contract (UDC).

```java
// Create a deployer instance
Deployer deployer = new StandardDeployer(address, provider, account);

// Create a deploment request and send it
Request<ContractDeployment> request = deployer.deployContract(
        classHash,
        unique,
        salt,
        List.of(constructorCalldata1, constructorCalldata2, ...));
ContractDeployment result = request.send();

// Get an address of the deployed contract
Request<Felt> addressRequest = deployer.findContractAddress(result);
Felt address = addressRequest.send();
```

or in Kotlin

```kotlin
// Create a deployer instance
val deployer: Deployer = StandardDeployer(address, provider, account)

// Create a deploment request and send it
val request = deployer.deployContract(
    classHash,
    unique,
    salt,
    listOf(constructorCalldata1, constructorCalldata2),
)
val result = request.send()

// Get an address of the deployed contract
val addressRequest = deployer.findContractAddress(result)
val (value) = addressRequest.send()
```

# Package com.swmansion.starknet.provider

Provider interface and its implementations.

```java
// Create a provider instance
Provider provider = ...

// Get a storage value request
Request<Felt> request = provider.getStorageAt(address, key);
// Send a request
Felt response = request.send();

// For most methods block hash, number or tag can be specified
Request<Felt> request = provider.getStorageAt(address, key, Felt.fromHex("0x123..."));
Request<Felt> request = provider.getStorageAt(address, key, 1234);
Request<Felt> request = provider.getStorageAt(address, key, BlockTag.LATEST);
```

# Package com.swmansion.starknet.provider.exceptions

Exceptions thrown by the StarkNet providers.

`Request.send()` throws `RequestFailedException` unchecked exception.
It can optionally be handled.

```java
Request<Felt> request = ...
// Send a request
try {
    Felt response = request.send();
} catch (RequestFailedException e) {
    // Handle an exception
}
```

In the case of `Request.sendAsync()`, an exception would have to be handled in the returned `CompletableFuture`.

# Package com.swmansion.starknet.provider.gateway

Provider utilising StarkNet gateway and feeder gateway for communication with the network.

```java
// Create a provider using GatewayProvider static methods
GatewayProvider.makeTestnetProvider();
// Chain id can be specified
GatewayProvider.makeTestnetProvider(StarknetChainId.TESTNET2);
// As well as the custom HttpService
GatewayProvider.makeTestnetProvider(myHttpService, StarknetChainId.TESTNET2);

// Provider can be also created using a constructor
new GatewayProvider("feederGatewayUrl", "gatewayUrl", StarknetChainId.TESTNET);
// or with a custom HttpService
new GatewayProvider("feederGatewayUrl", "gatewayUrl", StarknetChainId.TESTNET, myHttpService); 
```

# Package com.swmansion.starknet.provider.rpc

Provider implementing the [JSON RPC interface](https://github.com/starkware-libs/starknet-specs)
to communicate with the network.

```java
// JsonRpcProvider can only be created using constructor
new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);
// or with a custom HttpService
new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET, myHttpService);
```

# Package com.swmansion.starknet.service.http

Http service used to communicate with StarkNet.

You can create a `OkHttpService` yourself and pass it whenever creating a provider. This way your whole
application can use a single `OkHttpClient`. Read more [here](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared).

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
