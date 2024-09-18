# Module starknet-jvm

Starknet-jvm is a library allowing for easy interaction with the Starknet JSON-RPC nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, Starknet-jvm has been created with compatibility with Java in mind.

<!-- TOC -->
## Table of contents
* [Using provider](#using-provider)
* [Reusing provider](#reusing-providers)
* [Creating account](#creating-account)
* [Transferring STRK tokens](#transferring-strk-tokens)
* [Making synchronous requests](#making-synchronous-requests)
* [Making asynchronous requests](#making-asynchronous-requests)
* [Deploying account V3](#deploying-account-v3)
* [Estimating fee for deploy account V3 transaction](#estimating-fee-for-deploy-account-v3-transaction)
* [Deploying account V1](#deploying-account-v1)
* [Estimating fee for deploy account V1 transaction](#estimating-fee-for-deploy-account-v1-transaction)
* [Invoking contract: Transferring ETH](#invoking-contract-transferring-eth)
* [Estimating fee for invoke V3 transaction](#estimating-fee-for-invoke-v3-transaction)
* [Calling contract: Fetching ETH balance](#calling-contract-fetching-eth-balance)
* [Making multiple calls: get multiple transactions data](#making-multiple-calls-get-multiple-transactions-data)
* [Making multiple calls of different types in one request](#making-multiple-calls-of-different-types-in-one-request)
* [Declaring Cairo 1/2 contract V3](#declaring-cairo-12-contract-v3)
* [Estimating fee for declare V3 transaction](#estimating-fee-for-declare-v3-transaction)
* [Declaring Cairo 1/2 contract V2](#declaring-cairo-12-contract-v2)
* [Estimating fee for declare V2 transaction](#estimating-fee-for-declare-v2-transaction)
<!-- TOC -->

### Using provider
`Provider` is a facade for interacting with Starknet. `JsonRpcProvider` is a client which interacts with a Starknet full nodes like [Pathfinder](https://github.com/eqlabs/pathfinder), [Papyrus](https://github.com/starkware-libs/papyrus) or [Juno](https://github.com/NethermindEth/juno).
It supports read and write operations, like querying the blockchain state or adding new transactions.
```kotlin
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    val provider = JsonRpcProvider("https://your.node.url/rpc")
    
    val request = provider.getBlockWithTxs(1)
    val response = request.send()
}
```
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
Provider provider = new JsonRpcProvider("https://your.node.url/rpc");
Account account1 = new StandardAccount(provider, accountAddress1, privateKey1);
Account account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```
```kotlin
val provider = JsonRpcProvider("https://your.node.url/rpc")
val account1 = StandardAccount(provider, accountAddress1, privateKey1)
val account2 = StandardAccount(provider, accountAddress2, privateKey2)
```

❌ **Don't:**
```java
Provider provider1 = new JsonRpcProvider("https://your.node.url/rpc");
Account account1 = new StandardAccount(provider1, accountAddress1, privateKey1);
Provider provider2 = new JsonRpcProvider("https://your.node.url/rpc");
Account account2 = new StandardAccount(provider2, accountAddress2, privateKey2);
```
```kotlin
val provider1 = JsonRpcProvider("https://your.node.url/rpc")
val account1 = StandardAccount(provider1, accountAddress1, privateKey1)
val provider2 = JsonRpcProvider("https://your.node.url/rpc")
val account2 = StandardAccount(provider2, accountAddress2, privateKey2)
```

### Creating account
`StandardAccount` is the default implementation of `Account` interface. It supports an account contract which proxies the calls to other contracts on Starknet.

Account can be created in two ways:

- By constructor (It is required to provide an address and either private key or signer).

- By methods `Account.signDeployAccountV3()` or `Account.signDeployAccountV3()`

There are some examples how to do it:

```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId

fun main() {
    // If you don't have a private key, you can generate a random one
    val randomPrivateKey = StandardAccount.generatePrivateKey()

    // Create an instance of account which is already deployed
    // providing an address and a private key
    val account = StandardAccount(
        address = Felt.fromHex("0x123"),
        privateKey = Felt.fromHex("0x456"),
        provider = ...,
        chainId = StarknetChainId.SEPOLIA,
    )
    
    // It's possible to specify a signer
    val accountWithSigner = StandardAccount(
        address = Felt.fromHex("0x123"),
        signer = ...,
        provider = ...,
        chainId = StarknetChainId.SEPOLIA,
    )
}
```

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

### Transferring STRK tokens
```kotlin
import com.swmansion.starknet.account.StandardAccount
import com.swmansion.starknet.data.types.Call
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.data.types.StarknetChainId
import com.swmansion.starknet.data.types.Uint256
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    val provider = JsonRpcProvider("https://your.node.url")
    val account = StandardAccount(
        address = Felt.fromHex("0x123"),
        privateKey = Felt.fromHex("0x456"),
        provider = provider,
        chainId = StarknetChainId.SEPOLIA,
    )


    val amount = Uint256(Felt(100))
    val recipientAccountAddress = Felt.fromHex("0x789")
    val strkContractAddress = Felt.fromHex("0x04718f5a0fc34cc1af16a1cdee98ffb20c31f5cd61d6ab07201858f4287c938d")
    val call = Call(
        contractAddress = strkContractAddress,
        entrypoint = "transfer",
        calldata = listOf(recipientAccountAddress) + amount.toCalldata(),
    )

    val request = account.executeV3(call)
    val response = request.send()
}
```

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
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        Felt response = request.send();

        System.out.println(response);
    }
}
```

```kotlin
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://your.node.url/rpc")

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
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Make a request
        Felt contractAddress = Felt.fromHex("0x42362362436");
        Felt storageKey = Felt.fromHex("0x13241253414");
        Request<Felt> request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST);
        CompletableFuture<Felt> response = request.sendAsync();

        response.thenAccept(System.out::println);
    }
}
```

```kotlin
import com.swmansion.starknet.data.types.BlockTag
import com.swmansion.starknet.data.types.Felt
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://your.node.url/rpc")

    // Make an asynchronous request
    val contractAddress = Felt.fromHex("0x423623626")
    val storageKey = Felt.fromHex("0x132412414")
    val request = provider.getStorageAt(contractAddress, storageKey, BlockTag.LATEST)
    val future = request.sendAsync()

    future.thenAccept { println(it) }
}
```


## Deploying account V3
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeployAccountV3Transaction", language="kotlin") -->
```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.ContractAddressCalculator;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.util.List;

public class Main {
public static void main(String[] args) {
// Create a provider for interacting with Starknet
Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);
        // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.

        // Use the class hash of the desired account contract (i.e. the class hash of OpenZeppelin account contract)
        Felt classHash = Felt.fromHex("0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f");
        Felt salt = new Felt(789);
        List<Felt> calldata = List.of(publicKey);

        Felt address = ContractAddressCalculator.calculateAddressFromHash(
                classHash,
                calldata,
                salt
        );

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(address, privateKey, provider, chainId, Felt.ONE);

        ResourceBounds l1ResourceBounds = new ResourceBounds(
                new Uint64(20000),
                new Uint128(120000000000L)
        );

        DeployAccountParamsV3 params = new DeployAccountParamsV3(Felt.ZERO, l1ResourceBounds);

        // Make sure to prefund the new account address

        DeployAccountTransactionV3Payload payload = account.signDeployAccountV3(classHash, calldata, salt, params, false);

        DeployAccountResponse response = provider.deployAccount(payload).send();

        // Make sure the address matches the calculated one
        System.out.println("Calculated address: " + address);
        System.out.println("Deployed address: " + response.getAddress());
        System.out.println("Are addresses equal: " + address.equals(response.getAddress()));
    }
}
```

## Estimating fee for deploy account V3 transaction

<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeployAccountV3Transaction", language="kotlin") -->
```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;


import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        DeployAccountParamsV3 params = new DeployAccountParamsV3(
                Felt.ZERO,
                ResourceBounds.ZERO
        );
        
        Felt salt = new Felt(2);
        List<Felt> calldata = List.of(publicKey);
        // Use the class hash of the desired account contract (i.e. the class hash of OpenZeppelin account contract)
        Felt classHash = Felt.fromHex("0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f");
        DeployAccountTransactionV3Payload payloadForFeeEstimation = account.signDeployAccountV3(
                classHash,
                calldata,
                salt,
                params,
                true
        );
        
        Request<EstimateFeeResponseList> request = provider.getEstimateFee(List.of(payloadForFeeEstimation));
        EstimateFeeResponse response = request.send().getValues().get(0);

        System.out.println("The estimated fee is: " + response.getOverallFee().getValue() + ".");
    }
}
```

## Deploying account V1
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeployAccountV1Transaction", language="kotlin") -->
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
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);
        // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.

        // Use the class hash of the desired account contract (i.e. the class hash of OpenZeppelin account contract)
        Felt classHash = Felt.fromHex("0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f");
        Felt salt = new Felt(789);
        List<Felt> calldata = List.of(publicKey);

        Felt address = ContractAddressCalculator.calculateAddressFromHash(
                classHash,
                calldata,
                salt
        );

        StarknetChainId chainId = provider.getChainId().send();

        Account newAccount = new StandardAccount(address, privateKey, provider, chainId, Felt.ZERO);

        // Make sure to prefund the new account address with at least maxFee

        // Create and sign deploy account transaction
        DeployAccountTransactionV1Payload payload = newAccount.signDeployAccountV1(
                classHash,
                calldata,
                salt,
                // 10*fee from estimate deploy account fee
                Felt.fromHex("0x11fcc58c7f7000")
        );

        DeployAccountResponse response = provider.deployAccount(payload).send();

        Thread.sleep(15000); // wait for deploy account tx to complete
        
        // Make sure the address matches the calculated one
        System.out.println("Calculated address: " + address);
        System.out.println("Deployed address: " + response.getAddress());
        System.out.println("Are addresses equal: " + address.equals(response.getAddress()));

    }
}
```

## Estimating fee for deploy account V1 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeployAccountV1Transaction", language="kotlin") -->
```java
package org.example;

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
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);

        // Use the class hash of the desired account contract (i.e. the class hash of OpenZeppelin account contract)
        Felt classHash = Felt.fromHex("0x4d07e40e93398ed3c76981e72dd1fd22557a78ce36c0515f679e27f0bb5bc5f");
        Felt salt = Felt.ONE;
        List<Felt> calldata = List.of(publicKey);
        Felt accountAddress = ContractAddressCalculator.calculateAddressFromHash(
                classHash,
                calldata,
                salt
        );
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        DeployAccountTransactionV1Payload payloadForFeeEstimation = account.signDeployAccountV1(
                classHash,
                calldata,
                salt,
                Felt.ZERO,
                Felt.ZERO,
                true
        );

        Request<EstimateFeeResponseList> request = provider.getEstimateFee(List.of(payloadForFeeEstimation));
        EstimateFeeResponse feeEstimate = request.send().getValues().get(0);

        System.out.println("The estimated fee is: " + feeEstimate.getOverallFee().getValue() + ".");
    }
}
```

## Invoking contract: Transferring ETH
<!-- codeSection(path="network/account/AccountTest.kt", function="transferETH", language="kotlin") -->
```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.provider.Request;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");
        
        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        Felt recipientAccountAddress = Felt.fromHex("0x987654321");
        Uint256 amount = new Uint256(new Felt(451));
        
        // Specify the contract address, in this example ERC-20 ETH contract is used
        Felt erc20ContractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");
        
        // Create a call
        List<Felt> calldata = new ArrayList<>();
        calldata.add(recipientAccountAddress);
        calldata.addAll(amount.toCalldata());
        Call invokeCall = new Call(erc20ContractAddress, "transfer", calldata);

        // Create and sign invoke transaction
        Request<InvokeFunctionResponse> executeRequest = account.executeV3(invokeCall);

        // account.executeV3(call) estimates the fee automatically
        // If you want to estimate the fee manually, please refer to the "Estimate Fee" example
        
        // Send the transaction
        InvokeFunctionResponse executeResponse = executeRequest.send();

        Thread.sleep(20000); // wait for invoke tx to complete

        // Make sure that the transaction succeeded
        Request<? extends TransactionReceipt> invokeReceiptRequest = provider.getTransactionReceipt(executeResponse.getTransactionHash());
        TransactionReceipt invokeReceipt = invokeReceiptRequest.send();

        System.out.println("Was invoke transaction accepted? " + invokeReceipt.isAccepted() + ".");
    }
}
```

## Estimating fee for invoke V3 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForInvokeV3Transaction", language="kotlin") -->
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
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        Felt contractAddress = Felt.fromHex("0x123456789");

        // In this example we want to increase the balance by 10
        Call call = new Call(contractAddress, "increase_balance", List.of(new Felt(10)));

        Request<EstimateFeeResponseList> request = account.estimateFeeV3(
                call,
                false
        );

        EstimateFeeResponse feeEstimate = request.send().getValues().get(0);
        System.out.println("The estimated fee is: " + feeEstimate.getOverallFee().getValue() + ".");
    }
}
```

## Calling contract: Fetching ETH balance
<!-- codeSection(path="network/account/AccountTest.kt", function="getETHBalance", language="kotlin") -->
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
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");
        
        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
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

        System.out.println("Balance: " + balance.toString() + ".");
    }
}
```

## Making multiple calls: get multiple transactions data
<!-- codeSection(path="starknet/provider/ProviderTest.kt", function="batchGetTransactions", language="kotlin") -->
```java
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.RequestResult;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.service.http.requests.HttpBatchRequest;

import java.util.List;

import static com.swmansion.starknet.data.Selector.selectorFromName;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        JsonRpcProvider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Batch any RPC requests
        // Get block hash and number + Check the initial value of `balance` in contract
        HttpBatchRequest mixedRequest = provider.batchRequestsAny(
                provider.getBlockHashAndNumber(),
                provider.getStorageAt(Felt.fromHex("0x123"), selectorFromName("balance"))
        );

        // Create and send the batch request
        List<RequestResult> mixedResponse = mixedRequest.send();

        GetBlockHashAndNumberResponse blockHashAndNumber = (GetBlockHashAndNumberResponse) mixedResponse.get(0).getOrNull();
        Felt contractBalance = (Felt) mixedResponse.get(1).getOrNull();

        System.out.println("Block hash: " + blockHashAndNumber.getBlockHash() + ".");
        System.out.println("Initial contract balance: " + contractBalance.getValue() + ".");
    }
}
```

## Making multiple calls of different types in one request
<!-- codeSection(path="starknet/provider/ProviderTest.kt", function="batchRequestsAny", language="kotlin") -->
```java
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.RequestResult;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.service.http.requests.HttpBatchRequest;

import java.util.List;

import static com.swmansion.starknet.data.Selector.selectorFromName;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        JsonRpcProvider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Batch any RPC requests
        // Get block hash and number + Check the initial value of `balance` in contract
        HttpBatchRequest mixedRequest = provider.batchRequestsAny(
                provider.getBlockHashAndNumber(),
                provider.getStorageAt(Felt.fromHex("0x123"), selectorFromName("balance"))
        );

        // Create and send the batch request
        List<RequestResult> mixedResponse = mixedRequest.send();

        GetBlockHashAndNumberResponse blockHashAndNumber = (GetBlockHashAndNumberResponse) mixedResponse.get(0).getOrNull();
        Felt contractBalance = (Felt) mixedResponse.get(1).getOrNull();

        System.out.println("Block hash: " + blockHashAndNumber.getBlockHash() + ".");
        System.out.println("Initial contract balance: " + contractBalance.getValue() + ".");
    }
}
```

## Declaring Cairo 1/2 contract V3
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeclareV3Transaction", language="kotlin") -->
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
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

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

        Thread.sleep(60000); // wait for declare tx to complete

        TransactionReceipt receipt = provider.getTransactionReceipt(response.getTransactionHash()).send();
        System.out.println("Was transaction accepted? " + receipt.isAccepted() + ".");
    }
}
```

## Estimating fee for declare V3 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeclareV3Transaction", language="kotlin") -->
```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        // Import a compiled contract
        Path contractPath = Paths.get("contract.sierra.json");
        Path casmPath = Paths.get("contract.casm.json");
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));
        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);
        Felt nonce = account.getNonce().send();

        DeclareParamsV3 params = new DeclareParamsV3(
                nonce,
                ResourceBounds.ZERO
        );

        DeclareTransactionV3Payload payload = account.signDeclareV3(
                contractDefinition,
                casmContractDefinition,
                params,
                true
        );

        Request<EstimateFeeResponseList> request = provider.getEstimateFee(List.of(payload), Collections.emptySet());
        EstimateFeeResponse response = request.send().getValues().get(0);

        System.out.println("The estimated fee is: " + response.getOverallFee().getValue() + ".");
    }
}
```

## Declaring Cairo 1/2 contract V2
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="signAndSendDeclareV2Transaction", language="kotlin") -->
```java
package org.example;

import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        // Import a compiled contract
        Path contractPath = Paths.get("contract.sierra.json");
        Path casmPath = Paths.get("contract.casm.json");
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));
        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);
        Felt nonce = account.getNonce().send();

        DeclareTransactionV2Payload payload = account.signDeclareV2(
                contractDefinition,
                casmContractDefinition,
                new ExecutionParams(nonce, new Felt(1000000000000000L)),
                false
        );
        Request<DeclareResponse> request = provider.declareContract(payload);
        DeclareResponse response = request.send();

        Thread.sleep(60000);

        TransactionReceipt receipt = provider.getTransactionReceipt(response.getTransactionHash()).send();

        System.out.println("Was transaction accepted? " + receipt.isAccepted() + ".");
    }
}
```

## Estimating fee for declare V2 transaction
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="estimateFeeForDeclareV2Transaction", language="kotlin") -->
```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://your.node.url/rpc");

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.

        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(accountAddress, privateKey, provider, chainId, Felt.ZERO);

        // Import a compiled contract
        Path contractPath = Paths.get("contract.sierra.json");
        Path casmPath = Paths.get("contract.casm.json");
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));

        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);
        Felt nonce = account.getNonce().send();

        DeclareTransactionV2Payload payload = account.signDeclareV2(
                contractDefinition,
                casmContractDefinition,
                new ExecutionParams(nonce,Felt.ZERO),
                true
        );
        Request<EstimateFeeResponseList> request = provider.getEstimateFee(List.of(payload), Collections.emptySet());
        EstimateFeeResponseList response = request.send();
        EstimateFeeResponse feeEstimate = response.getValues().get(0);
        
        System.out.println("The estimated fee is: " + feeEstimate.getOverallFee().getValue() + ".");
    }
}
```

# Package com.swmansion.starknet.account
Account interface used to simplify preparing, signing Starknet transactions and automatic fee estimation.


## Example usage of `StandardAccount`
<!-- codeSection(path="starknet/account/StandardAccountTest.kt", function="exampleAccountUsage", language="kotlin") -->

# Package com.swmansion.starknet.crypto

Cryptography and signature related classes.
This is a low level module. Recommended way of using is through
Signer and Account implementations.

# Package com.swmansion.starknet.data
Data classes representing Starknet objects and utilities for handling them.

# Package com.swmansion.starknet.data.types
Data classes representing Starknet objects and transactions.

# Package com.swmansion.starknet.deployercontract
Classes for interacting with Universal Deployer Contract (UDC).

<!-- codeSection(path="starknet/deployercontract/DeployerContractTest.kt", function="testUdcDeployV3", language="kotlin") -->

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
Recommended way of using Signer is through an [Account](src/main/kotlin/com/swmansion/starknet/account/Account.kt).

```java
// Create a signer
Signer signer = ...
        
// Sign a transaction
List<Felt> signature = signer.signTransaction(tx);

// Get a public key
Felt publicKey = signer.getPublicKey();
```

