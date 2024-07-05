# Module starknet-jvm

Starknet-jvm is a library allowing for easy interaction with the Starknet JSON-RPC nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, Starknet-jvm has been created with compatibility with Java in mind.

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
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        Account account = new StandardAccount(provider, accountAddress, privateKey);

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
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

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
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Create an account interface
        Felt accountAddress = Felt.fromHex("0x13241455");
        Felt privateKey = Felt.fromHex("0x425125");
        Account account = new StandardAccount(provider, accountAddress, privateKey);

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
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")
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

## Deploying account V3

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.ContractAddressCalculator;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Create an account interface
        Felt privateKey = Felt.fromHex("0x123");
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

        StarknetChainId chainId = provider.getChainId().send();

        Account account = new StandardAccount(address, privateKey, provider, chainId, Felt.ZERO);

        // Estimate fee for declaring a contract
        DeployAccountParamsV3 paramsForFeeEstimate = new DeployAccountParamsV3(Felt.ZERO, ResourceBounds.ZERO);
        DeployAccountTransactionV3Payload payloadForFeeEstimate = account.signDeployAccountV3(
                classHash,
                calldata,
                salt,
                paramsForFeeEstimate,
                true
        );
        Request<EstimateFeeResponseList> feeEstimateRequest = provider.getEstimateFee(List.of(payloadForFeeEstimate));

        EstimateFeeResponse feeEstimate = feeEstimateRequest.send().getValues().get(0);
        ResourceBounds l1ResourceBounds = feeEstimate.toResourceBounds(1.5, 1.5).getL1Gas();

        DeployAccountParamsV3 params = new DeployAccountParamsV3(Felt.ZERO, l1ResourceBounds);
        DeployAccountTransactionV3Payload payload = account.signDeployAccountV3(classHash, calldata, salt, params, false);

        DeployAccountResponse response = provider.deployAccount(payload).send();
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    // Create an account interface
    val privateKey = Felt.fromHex("0x123")
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

    val chainId = provider.getChainId().send()

    val account = StandardAccount(
        address,
        privateKey,
        provider,
        chainId,
    )

    val payloadForFeeEstimate = account.signDeployAccountV3(
        classHash = classHash,
        salt = salt,
        calldata = calldata,
        params = DeployAccountParamsV3(
            nonce = Felt.ZERO,
            l1ResourceBounds = ResourceBounds.ZERO,
        ),
        forFeeEstimate = true,
    )
    val feeEstimateRequest = provider.getEstimateFee(listOf(payloadForFeeEstimate))

    val feeEstimate = feeEstimateRequest.send().values.first()
    val resourceBounds = feeEstimate.toResourceBounds()

    val params = DeployAccountParamsV3(
        nonce = Felt.ZERO,
        l1ResourceBounds = resourceBounds.l1Gas,
    )
    val payload = account.signDeployAccountV3(
        classHash = classHash,
        salt = salt,
        calldata = calldata,
        params = params,
    )

    // Create and sign deploy account transaction
    val response = provider.deployAccount(payload).send()
}
```

## Deploying account V1

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.ContractAddressCalculator;
import com.swmansion.starknet.data.types.DeployAccountResponse;
import com.swmansion.starknet.data.types.DeployAccountTransactionV1Payload;
import com.swmansion.starknet.data.types.Felt;
import com.swmansion.starknet.data.types.StarknetChainId;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Create an account interface
        Felt privateKey = Felt.fromHex("0x123");
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

        StarknetChainId chainId = provider.getChainId().send();

        Account account = new StandardAccount(address, privateKey, provider, chainId, Felt.ZERO);

        // Make sure to prefund the address with at least maxFee

        // Create and sign deploy account transaction
        DeployAccountTransactionV1Payload payload = account.signDeployAccountV1(
                classHash,
                calldata,
                salt,
                // 10*fee from estimate deploy account fee
                Felt.fromHex("0x11fcc58c7f7000")
        );

        DeployAccountResponse response = provider.deployAccount(payload).send();
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.crypto.StarknetCurve
import com.swmansion.starknet.data.ContractAddressCalculator
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    // Create an account interface
    val privateKey = Felt.fromHex("0x123")
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

    val chainId = provider.getChainId().send()

    val account = StandardAccount(
        address,
        privateKey,
        provider,
        chainId,
    )

    val payload = account.signDeployAccountV1(
        classHash = classHash,
        salt = salt,
        calldata = calldata,
        maxFee = Felt.fromHex("0x11fcc58c7f7000"),
    )

    // Create and sign deploy account transaction
    val response = provider.deployAccount(payload).send()
}
```

## Invoking contract: Transferring ETH

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.provider.Request;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        StarknetChainId chainId = provider.getChainId().send();

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");

        // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        Felt recipientAccountAddress = Felt.fromHex("0x987654321");
        Uint256 amount = new Uint256(new Felt(451));

        // Specify the contract address, in this example ETH ERC20 contract is used
        Felt contractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");

        // Create a call
        List<Felt> calldata = List.of(recipientAccountAddress, amount.getLow(), amount.getHigh()); // amount is Uint256 and is represented by two Felt values
        Call call = new Call(contractAddress, "transfer", calldata);

        // Make sure to prefund the account with enough funds to cover the transaction fee and the amount to be transferred

        // Create and sign invoke transaction
        Request<InvokeFunctionResponse> request = account.executeV3(call);

        // Send the transaction
        InvokeFunctionResponse response = request.send();
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Uint256
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    val chainId = provider.getChainId().send()

    // Set up an account
    val privateKey = Felt.fromHex("0x123")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider, chainId)

    val recipientAccountAddress = Felt.fromHex("0x987654321")
    // Make sure to prefund the account with enough funds to cover the transaction fee and the amount to be transferred
    // account.execute(Call) estimates the fee automatically
    // If you want to estimate the fee manually, please refer to the "Estimate Fee" example
    val amount = Uint256(Felt(451))

    // Specify the contract address, in this example ETH ERC20 contract is used
    val contractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")

    // Create a call
    val calldata = listOf(recipientAccountAddress, amount.low, amount.high) // amount is Uint256 and is represented by two Felt values
    val call = Call(
        contractAddress = contractAddress,
        entrypoint = "transfer",
        calldata = calldata,
    )

    // Make sure to prefund the account with enough funds to cover the transaction fee and the amount to be transferred

    // Create and sign invoke transaction
    val request = account.executeV3(call)

    // Send the transaction
    val response = request.send()
}
```

## Calling contract: Fetching ETH balance

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        StarknetChainId chainId = provider.getChainId().send();

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and key are for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        // Specify the contract address, in this example ETH ERC20 contract is used
        Felt contractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");

        // Create a call
        List<Felt> calldata = List.of(account.getAddress());
        Call call = new Call(contractAddress, "balanceOf", calldata);
        Request<FeltArray> request = provider.callContract(call);

        // Send the call request
        List<Felt> response = request.send();

        // Output value's type is Uint256 and is represented by two Felt values
        Uint256 balance = new Uint256(response.get(0), response.get(1));
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Uint256
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    val chainId = provider.getChainId().send()

    // Set up an account
    val privateKey = Felt.fromHex("0x123")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider, chainId)

    // Specify the contract address, in this example ETH ERC20 contract is used
    val contractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")

    // Create a call
    val calldata = listOf(account.address)
    val call = Call(
        contractAddress = contractAddress,
        entrypoint = "balanceOf",
        calldata = calldata,
    )
    val request = provider.callContract(call)

    // Send the call request
    val response: List<Felt> = request.send()

    //Output value's type is Uint256 and is represented by two Felt values
    val balance = Uint256(
        low = response[0],
        high = response[1],
    )
}
```

## Calling multiple contracts: Fetching ETH balance

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.RequestResult;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        JsonRpcProvider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Get the chain ID
        StarknetChainId chainId = provider.getChainId().send();

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and key are for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        // Specify the contract addresses, in this example ETH ERC20 contracts are used
        Felt contractAddress1 = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");
        Felt contractAddress2 = Felt.fromHex("0x05dcf20d87407f0f7a6bd081a2cee990b19c77ebf31c8b0ddd5a6f931af21e0c");

        // Create multiple calls
        List<Felt> calldata = List.of(account.getAddress());

        Call call1 = new Call(contractAddress1, "balanceOf", calldata);
        Call call2 = new Call(contractAddress2, "balanceOf", calldata);

        // Create a batch request and send it
        List<RequestResult<FeltArray>> response = provider.batchRequests(
                provider.callContract(call1),
                provider.callContract(call2)
        ).send();

        // Access output values from the batch response
        Uint256 balance1 = new Uint256(response.get(0).getOrThrow().get(0), response.get(0).getOrThrow().get(1));
        Uint256 balance2 = new Uint256(response.get(1).getOrThrow().get(0), response.get(1).getOrThrow().get(1));
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.Uint256
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main(args: Array<String>) {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    val chainId = provider.getChainId().send()

    // Set up an account
    val privateKey = Felt.fromHex("0x123")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider, chainId)

    // Specify the contract addresses, in this example ETH ERC20 contract is used
    val contractAddress1 = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7")
    val contractAddress2 = Felt.fromHex("0x05dcf20d87407f0f7a6bd081a2cee990b19c77ebf31c8b0ddd5a6f931af21e0c")

    // Create multiple calls
    val calldata = listOf(account.address)
    val call1 = Call(
        contractAddress = contractAddress1,
        entrypoint = "balanceOf",
        calldata = calldata,
    )
    val call2 = Call(
        contractAddress = contractAddress2,
        entrypoint = "balanceOf",
        calldata = calldata,
    )

    // Create a batch request
    val batchRequest = provider.batchRequests(
        provider.callContract(call1),
        provider.callContract(call2),
    )

    // Send the batch call request
    val batchResponse = batchRequest.send()

    // Access output values from batch the response
    val balance1 = Uint256(
        low = batchResponse[0].getOrThrow()[0],
        high = batchResponse[0].getOrThrow()[1],
    )
    val balance2 = Uint256(
        low = batchResponse[1].getOrThrow()[0],
        high = batchResponse[1].getOrThrow()[1],
    )
}
```

## Making multiple calls of different types in one request

### In Java

```java
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.RequestResult;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.service.http.requests.HttpRequest;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        JsonRpcProvider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        // Create requests
        Request<Transaction> txRequest1 = provider.getTransaction(Felt.fromHex("0x0123"));
        Request<Transaction> txRequest2 = provider.getTransaction(Felt.fromHex("0x0456"));
        Request<GetTransactionStatusResponse> txStatusRequest = provider.getTransactionStatus(Felt.fromHex("0x0789"));
        Request<BlockWithTransactionHashes> blockWithTxHashesRequest = provider.getBlockWithTxHashes(Felt.fromHex("0x0abc"));

        // Create a batch request of different types and send it
        List<RequestResult<StarknetResponse>> response = provider.batchRequestsAny(
                (HttpRequest<? extends StarknetResponse>) txRequest1,
                (HttpRequest<? extends StarknetResponse>) txRequest2,
                (HttpRequest<? extends StarknetResponse>) txStatusRequest,
                (HttpRequest<? extends StarknetResponse>) blockWithTxHashesRequest
        ).send();

        // Access output values from the response
        Transaction tx1 = (Transaction) response.get(0).getOrThrow();
        Transaction tx2 = (Transaction) response.get(1).getOrThrow();
        GetTransactionStatusResponse txStatus = (GetTransactionStatusResponse) response.get(2).getOrThrow();
        BlockWithTransactionHashes blockWithTxHashes = (BlockWithTransactionHashes) response.get(3).getOrThrow();
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.data.types.BlockWithTransactionHashes
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.GetTransactionStatusResponse
import com.swmansion.starknet.data.types.Transaction
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main(args: Array<String>) {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    // Create requests
    val txRequest1 = provider.getTransaction(Felt.fromHex("0x0123"))
    val txRequest2 = provider.getTransaction(Felt.fromHex("0x0456"))
    val txStatusRequest = provider.getTransactionStatus(Felt.fromHex("0x0789"))
    val blockWithTxHashesRequest = provider.getBlockWithTxHashes(Felt.fromHex("0x0abc"))

    val batchRequest = provider.batchRequestsAny(
        txRequest1,
        txRequest2,
        txStatusRequest,
        blockWithTxHashesRequest
    )

    // Send the batch request
    val response = batchRequest.send()

    // Access output values from the response
    val tx1 = response[0].getOrThrow() as Transaction
    val tx2 = response[1].getOrThrow() as Transaction
    val txStatus = response[2].getOrThrow() as GetTransactionStatusResponse
    val blockWithTxHashes = response[3].getOrThrow() as BlockWithTransactionHashes
}
```

## Declaring Cairo 1/2 contract V3

### In Java

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

        StarknetChainId chainId = provider.getChainId().send();

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        // Import a compiled contract
        Path contractPath = Paths.get("contract.sierra.json");
        Path casmPath = Paths.get("contract.casm.json");
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));
        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);
        Felt nonce = account.getNonce().send();

        // Estimate fee for declaring a contract
        DeclareTransactionV3Payload declareTransactionPayloadForFeeEstimate = account.signDeclareV3(contractDefinition, casmContractDefinition, new DeclareParamsV3(nonce, ResourceBounds.ZERO), true);
        Request<EstimateFeeResponseList> feeEstimateRequest = provider.getEstimateFee(List.of(declareTransactionPayloadForFeeEstimate));
        EstimateFeeResponse feeEstimate = feeEstimateRequest.send().getValues().get(0);
        // Make sure to prefund the account with enough funds to cover the fee for declare transaction

        // Declare a contract
        ResourceBounds l1ResourceBounds = feeEstimate.toResourceBounds(1.5, 1.5).getL1Gas();
        DeclareParamsV3 params = new DeclareParamsV3(nonce, l1ResourceBounds);
        DeclareTransactionV3Payload declareTransactionPayload = account.signDeclareV3(contractDefinition, casmContractDefinition, params, false);

        Request<DeclareResponse> request = provider.declareContract(declareTransactionPayload);
        DeclareResponse response = request.send();
        Felt hash = response.getTransactionHash(); // Hash of the transaction that declares a contract
    }
}
```

### In Kotlin

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.data.types.Felt.Companion.fromHex
import com.swmansion.starknet.provider.Provider
import com.swmansion.starknet.provider.rpc.JsonRpcProvider
import java.nio.file.Path
import kotlin.io.path.readText


fun main() {
    // Create a provider for interacting with Starknet
    val provider: Provider = JsonRpcProvider("https://example-node-url.com/rpc")

    val chainId = provider.getChainId().send()

    // Set up an account
    val privateKey = fromHex("0x1234")
    val accountAddress = fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO)

    // Import a compiled contract
    val contractCode = Path.of("contract.sierra.json").readText()
    val casmCode = Path.of("contract.casm.json").readText()
    val contractDefinition = Cairo1ContractDefinition(contractCode)
    val casmContractDefinition = CasmContractDefinition(casmCode)
    val nonce = account.getNonce().send()

    // Estimate fee for declaring a contract
    val declareTransactionPayloadForFeeEstimate = account.signDeclareV3(
        contractDefinition,
        casmContractDefinition,
        DeclareParamsV3(nonce, ResourceBounds.ZERO),
        true,
    )
    val feeEstimateRequest = provider.getEstimateFee(listOf(declareTransactionPayloadForFeeEstimate))
    val feeEstimate = feeEstimateRequest.send().values.first()

    // Make sure to prefund the account with enough funds to cover the fee for declare transaction

    // Declare a contract
    val l1ResourceBounds = feeEstimate.toResourceBounds(1.5, 1.5).l1Gas
    val params = DeclareParamsV3(nonce, l1ResourceBounds)
    val declareTransactionPayload = account.signDeclareV3(contractDefinition, casmContractDefinition, params, false)

    val request = provider.declareContract(declareTransactionPayload)
    val response = request.send()
    val hash = response.transactionHash // Hash of the transaction that declares a contract
}
```

# Package com.swmansion.starknet.account

Account interface used to simplify preparing, signing Starknet transactions and automatic fee estimation.

### Example usages of `StandardAccount`

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.TypedData;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.InvokeTransactionV3Payload;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc");

            StarknetChainId chainId = provider.getChainId().send();
        Felt address = new Felt(0x1234);
        Felt privateKey = new Felt(0x1);
        Account account = new StandardAccount(address, privateKey, provider, chainId);

        // Execute a single call
        ResourceBounds resourceBounds = new ResourceBounds(
                new Uint64(10000),
                new Uint128(10000000L)
        );
        Felt contractAddress = new Felt(0x1111);
        Call call = new Call(contractAddress, "increase_balance", List.of(new Felt(100)));
        Request<InvokeFunctionResponse> request = account.executeV3(call, resourceBounds);
        InvokeFunctionResponse response = request.send();

        // Execute multiple calls
        Call call1 = new Call(contractAddress, "increase_balance", List.of(new Felt(100)));
        Call call2 = new Call(contractAddress, "increase_balance", List.of(new Felt(200)));
        account.executeV3(List.of(call1, call2), resourceBounds).send();

        // Use automatic maxFee estimation
        account.executeV3(call).send();
        // or 
        account.executeV3(call, resourceBounds).send();

        // Construct transaction step by step
        Call otherCall = new Call(contractAddress, "increase_balance", List.of(new Felt(100)));

        Felt nonce = account.getNonce().send();
        InvokeParamsV3 params = new InvokeParamsV3(nonce, new ResourceBounds(new Uint64(20000), new Uint128(1200000000)));

        InvokeTransactionV3Payload signedTransaction = account.signV3(otherCall, params);
        InvokeFunctionResponse signedInvokeResponse = provider.invokeFunction(signedTransaction).send();

        // Sign transaction for fee estimation only
        InvokeTransactionV3Payload transactionForFeeEstimation = account.signV3(call, params, true);

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
import com.swmansion.starknet.account.Account
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.TypedData.Companion.fromJsonString
import com.swmansion.starknet.data.types.*
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    val provider = JsonRpcProvider("https://example-node-url.com/rpc")

    val chainId = provider.getChainId().send()
    val address = Felt(0x1234)
    val privateKey = Felt(0x1)
    val account: Account = StandardAccount(address, privateKey, provider, chainId)

    // Execute a single call
    val resourceBounds = ResourceBounds(
        Uint64(10000),
        Uint128(10000000L),
    )
    val contractAddress = Felt(0x1111)
    val call = Call(contractAddress, "increase_balance", listOf(Felt(100)))
    val request = account.executeV3(call, resourceBounds)
    val response = request.send()

    // Execute multiple calls
    val call1 = Call(contractAddress, "increase_balance", listOf(Felt(100)))
    val call2 = Call(contractAddress, "increase_balance", listOf(Felt(200)))
    account.executeV3(listOf(call1, call2), resourceBounds).send()

    // Use automatic maxFee estimation
    account.executeV3(call).send()

    // or 
    account.executeV3(call, resourceBounds).send()

    // Construct transaction step by step
    val otherCall = Call(contractAddress, "increase_balance", listOf(Felt(100)))

    val nonce = account.getNonce().send()
    val params = InvokeParamsV3(
        nonce,
        ResourceBounds(
            Uint64(20000),
            Uint128(1200000000),
        ),
    )

    val signedTransaction= account.signV3(otherCall, params)
    val signedInvokeResponse = provider.invokeFunction(signedTransaction).send()

    // Sign transaction for fee estimation only
    val transactionForFeeEstimation = account.signV3(call, params, true)

    // Sign and verify TypedData signature
    val typedData = fromJsonString("...")
    val typedDataSignature = account.signTypedData(typedData)
    val isValidSignatureRequest = account.verifyTypedDataSignature(typedData, typedDataSignature)
    val isValidSignature = isValidSignatureRequest.send()
}
```

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

```java
// Create a deployer instance
Deployer deployer = new StandardDeployer(address, provider, account);

// Create a deployment request and send it
Request<ContractDeployment> request = deployer.deployContractV3(
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
val deployer = StandardDeployer(address, provider, account)

// Create a deployment request and send it
val request = deployer.deployContractV3(
    classHash = classHash,
    unique = unique,
    salt = salt,
    constructorCalldata = List.of(constructorCalldata1, constructorCalldata2, ...),
    l1ResourceBounds = ResourceBounds(
        maxAmount = ...,
        maxPricePerUnit = ...,
    ),
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

Exceptions thrown by the Starknet providers.

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

# Package com.swmansion.starknet.provider.rpc

Provider implementing the [JSON RPC interface](https://github.com/starkware-libs/starknet-specs)
to communicate with the network.

```java
// JsonRpcProvider can only be created using constructor
new JsonRpcProvider("https://example-node-url.com/rpc");
// or with a custom HttpService
new JsonRpcProvider("https://example-node-url.com/rpc", myHttpService);
```

# Package com.swmansion.starknet.service.http

Http service used to communicate with Starknet.

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
