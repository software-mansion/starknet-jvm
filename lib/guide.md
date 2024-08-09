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

