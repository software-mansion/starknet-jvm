# Module starknet-jvm

Starknet-jvm is a library allowing for easy interaction with the Starknet JSON-RPC nodes, including
querying starknet state, executing transactions and deploying contracts.

Although written in Kotlin, Starknet-jvm has been created with compatibility with Java in mind.

## Quickstart
### Using provider
`Provider` is a facade for interacting with Starknet. `JsonRpcProvider` is a client which interacts with a Starknet full nodes like [Pathfinder](https://github.com/eqlabs/pathfinder), [Papyrus](https://github.com/starkware-libs/papyrus) or [Juno](https://github.com/NethermindEth/juno).
It supports read and write operations, like querying the blockchain state or adding new transactions.
```kotlin
import com.swmansion.starknet.provider.rpc.JsonRpcProvider

fun main() {
    val provider = JsonRpcProvider("https://your.node.url")
    
    val request = provider.getBlockWithTxs(1)
    val response = request.send()
}
```


### Reusing providers
Make sure you don't create a new provider every time you want to use one. Instead, you should reuse existing instance.
This way you reuse connections and thread pools.

✅ **Do:**

```kotlin
val provider = JsonRpcProvider("https://example-node-url.com/rpc")
val account1 = StandardAccount(provider, accountAddress1, privateKey1)
val account2 = StandardAccount(provider, accountAddress2, privateKey2)
```

❌ **Don't:**

```kotlin
val provider1 = JsonRpcProvider("https://example-node-url.com/rpc")
val account1 = StandardAccount(provider1, accountAddress1, privateKey1)
val provider2 = JsonRpcProvider("https://example-node-url.com/rpc")
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



### Using account - transferring STRK tokens
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



## Making synchronous requests



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
```kotlin
val privateKey = Felt(22222)
val publicKey = StarknetCurve.getPublicKey(privateKey)

val salt = Felt(2)
val calldata = listOf(publicKey)
val address = ContractAddressCalculator.calculateAddressFromHash(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
)

val newAccount = StandardAccount(
    address,
    privateKey,
    provider,
    chainId,
)
val l1ResourceBounds = ResourceBounds(
    maxAmount = Uint64(20000),
    maxPricePerUnit = Uint128(120000000000),
)
val params = DeployAccountParamsV3(
    nonce = Felt.ZERO,
    l1ResourceBounds = l1ResourceBounds,
)

// Prefund the new account address with STRK
val payload = newAccount.signDeployAccountV3(
    classHash = accountContractClassHash,
    salt = salt,
    calldata = calldata,
    params = params,
    forFeeEstimate = false,
)

val response = provider.deployAccount(payload).send()
// Make sure tx matches what we sent
val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV3
// Invoke function to make sure the account was deployed properly
val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))
val result = newAccount.executeV3(call).send()

val receipt = provider.getTransactionReceipt(result.transactionHash).send()
```


## Estimating fee for deploy account V3 transaction

```kotlin
val privateKey = Felt(22223)
val publicKey = StarknetCurve.getPublicKey(privateKey)

val salt = Felt(2)
val calldata = listOf(publicKey)
val address = ContractAddressCalculator.calculateAddressFromHash(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
)
val account = StandardAccount(
    address,
    privateKey,
    provider,
    chainId,
)
val params = DeployAccountParamsV3(
    nonce = Felt.ZERO,
    l1ResourceBounds = ResourceBounds.ZERO,
)
val payloadForFeeEstimation = account.signDeployAccountV3(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
    params = params,
    forFeeEstimate = true,
)
val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
```


## Deploying account V1
```kotlin
val privateKey = Felt(11111)
val publicKey = StarknetCurve.getPublicKey(privateKey)

val salt = Felt.ONE
val calldata = listOf(publicKey)
val address = ContractAddressCalculator.calculateAddressFromHash(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
)

// Make sure to prefund the new account address with ETH
val account = StandardAccount(
    address,
    privateKey,
    provider,
    chainId,
)
val payload = account.signDeployAccountV1(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
    // 10*fee from estimate deploy account fee
    maxFee = Felt.fromHex("0x11fcc58c7f7000"),
)

val response = provider.deployAccount(payload).send()
val tx = provider.getTransaction(response.transactionHash).send() as DeployAccountTransactionV1
// Invoke function to make sure the account was deployed properly
val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))
val result = account.executeV1(call).send()

val receipt = provider.getTransactionReceipt(result.transactionHash).send()
```


## Estimating fee for deploy account V1 transaction
```kotlin
val privateKey = Felt(11112)
val publicKey = StarknetCurve.getPublicKey(privateKey)

val salt = Felt.ONE
val calldata = listOf(publicKey)
val address = ContractAddressCalculator.calculateAddressFromHash(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
)

val account = StandardAccount(
    address,
    privateKey,
    provider,
    chainId,
)
val payloadForFeeEstimation = account.signDeployAccountV1(
    classHash = accountContractClassHash,
    calldata = calldata,
    salt = salt,
    maxFee = Felt.ZERO,
    nonce = Felt.ZERO,
    forFeeEstimate = true,
)
val feePayload = provider.getEstimateFee(listOf(payloadForFeeEstimation)).send()
```


## Invoking contract: Transferring ETH
```kotlin
val account = standardAccount

val recipientAccountAddress = constNonceAccountAddress

val amount = Uint256(Felt.ONE)
val call = Call(
    contractAddress = ethContractAddress,
    entrypoint = "transfer",
    calldata = listOf(recipientAccountAddress) + amount.toCalldata(),
)

val request = account.executeV1(call)
val response = request.send()
Thread.sleep(15000)

val transferReceipt = provider.getTransactionReceipt(response.transactionHash).send()
```


## Estimating fee for invoke V3 transaction
```kotlin
val call = Call(balanceContractAddress, "increase_balance", listOf(Felt(10)))

val request = account.estimateFeeV3(
    listOf(call),
    skipValidate = false,
)
val feeEstimate = request.send().values.first()
```


## Calling contract: Fetching ETH balance
```kotlin
val account = constNonceAccount
val call = Call(
    contractAddress = ethContractAddress,
    entrypoint = "balanceOf",
    calldata = listOf(account.address),
)

val request = provider.callContract(call)
val response = request.send()
val balance = Uint256(
    low = response[0],
    high = response[1],
)
```


## Making multiple calls: get multiple transactions data
```kotlin
val blockNumber = provider.getBlockNumber().send().value
val request = provider.batchRequests(
    provider.getTransactionByBlockIdAndIndex(blockNumber, 0),
    provider.getTransaction(invokeTransactionHash),
    provider.getTransaction(declareTransactionHash),
    provider.getTransaction(deployAccountTransactionHash),

)

val response = request.send()
```


## Making multiple calls of different types in one request
```kotlin
val request = provider.batchRequestsAny(
    provider.getTransaction(invokeTransactionHash),
    provider.getBlockNumber(),
    provider.getTransactionStatus(invokeTransactionHash),
)

val response = request.send()

val transaction = response[0].getOrThrow() as Transaction
val blockNumber = (response[1].getOrThrow() as IntResponse).value
val txStatus = response[2].getOrThrow() as GetTransactionStatusResponse
```


## Declaring Cairo 1/2 contract V3
```kotlin
ScarbClient.buildSaltedContract(
    placeholderContractPath = Path.of("src/test/resources/contracts_v2/src/placeholder_counter_contract.cairo"),
    saltedContractPath = Path.of("src/test/resources/contracts_v2/src/salted_counter_contract.cairo"),
)
val contractCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.sierra.json").readText()
val casmCode = Path.of("src/test/resources/contracts_v2/target/release/ContractsV2_SaltedCounterContract.casm.json").readText()

val contractDefinition = Cairo2ContractDefinition(contractCode)
val contractCasmDefinition = CasmContractDefinition(casmCode)
val nonce = account.getNonce().send()

val params = DeclareParamsV3(
    nonce = nonce,
    l1ResourceBounds = ResourceBounds(
        maxAmount = Uint64(100000),
        maxPricePerUnit = Uint128(1000000000000),
    ),
)
val declareTransactionPayload = account.signDeclareV3(
    contractDefinition,
    contractCasmDefinition,
    params,
)
val request = provider.declareContract(declareTransactionPayload)
val result = request.send()

val receipt = provider.getTransactionReceipt(result.transactionHash).send()
```


## Estimating fee for declare V3 transaction
```kotlin
val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

val contractDefinition = Cairo1ContractDefinition(contractCode)
val contractCasmDefinition = CasmContractDefinition(casmCode)
val nonce = account.getNonce().send()

val params = DeclareParamsV3(nonce = nonce, l1ResourceBounds = ResourceBounds.ZERO)
val declareTransactionPayload = account.signDeclareV3(
    contractDefinition,
    contractCasmDefinition,
    params,
    true,
)
val request = provider.getEstimateFee(payload = listOf(declareTransactionPayload), simulationFlags = emptySet())
val feeEstimate = request.send().values.first()
```


## Declaring Cairo 1/2 contract V2
```kotlin
val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

val contractDefinition = Cairo1ContractDefinition(contractCode)
val contractCasmDefinition = CasmContractDefinition(casmCode)
val nonce = account.getNonce().send()

val declareTransactionPayload = account.signDeclareV2(
    contractDefinition,
    contractCasmDefinition,
    ExecutionParams(nonce, Felt(5000000000000000L)),
)
val request = provider.declareContract(declareTransactionPayload)
val result = request.send()

val receipt = provider.getTransactionReceipt(result.transactionHash).send()
```


## Estimating fee for declare V2 transaction
```kotlin
val contractCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.sierra.json").readText()
val casmCode = Path.of("src/test/resources/contracts_v1/target/release/ContractsV1_HelloStarknet.casm.json").readText()

val contractDefinition = Cairo1ContractDefinition(contractCode)
val contractCasmDefinition = CasmContractDefinition(casmCode)
val nonce = account.getNonce().send()

val declareTransactionPayload = account.signDeclareV2(
    sierraContractDefinition = contractDefinition,
    casmContractDefinition = contractCasmDefinition,
    params = ExecutionParams(nonce, Felt.ZERO),
    forFeeEstimate = true,
)
val request = provider.getEstimateFee(payload = listOf(declareTransactionPayload), simulationFlags = emptySet())
val feeEstimate = request.send().values.first()
```


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



# Package com.swmansion.starknet.signer

Signer interface and its implementations.
Recommended way of using Signer is through an Account.



