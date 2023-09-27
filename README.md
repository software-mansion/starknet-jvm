<h2 align="center">☕ starknet jvm ☕</h2>

[Starknet](https://starkware.co/starknet/#:~:text=Live%20on%20Mainnet-,What%20is%20Starknet%3F,-Starknet%20is%20a) SDK for JVM languages:
- Java
- Kotlin
- Scala
- Clojure
- Groovy

## Table of contents

<!-- TOC -->
  * [Installation](#installation)
  * [Documentation](#documentation)
  * [Example usages](#example-usages)
    * [Making synchronous requests](#making-synchronous-requests)
    * [Making asynchronous requests](#making-asynchronous-requests)
  * [Development](#development)
    * [Hooks](#hooks)
    * [Ensuring idiomatic Java code](#ensuring-idiomatic-java-code)
  * [Building documentation](#building-documentation)
<!-- TOC -->

## Installation

Select the latest version from [the list](https://search.maven.org/artifact/com.swmansion.starknet/starknet) and follow installation instructions.

## Documentation

Documentation is provided in two formats:

- [Java and other jvm languages](https://docs.swmansion.com/starknet-jvm/)
- [Kotlin](https://docs.swmansion.com/starknet-jvm/kotlin/)


## Example usages

### Making synchronous requests

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
        // Create a provider for interacting with Starknet
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

### Making asynchronous requests

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
        // Create a provider for interacting with Starknet
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

### Deploying account

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.ContractAddressCalculator;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.*;
import com.swmansion.starknet.data.types.transactions.TransactionReceipt;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.math.BigInteger;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

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

        Account account = new StandardAccount(address, privateKey, provider, Felt.ZERO);
        
        Felt maxFee = Felt.fromHex("0x11fcc58c7f7000");  // should be 10*fee from estimate deploy account fee

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
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

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

    val account = StandardAccount(
        address,
        privateKey,
        provider,
    )

    val payload = account.signDeployAccount(
        classHash = classHash,
        salt = salt,
        calldata = calldata,
        maxFee = Felt.fromHex("0x11fcc58c7f7000"),  // should be 10*fee from estimate deploy account fee
    )
    
    // Create and sign deploy account transaction
    val response = provider.deployAccount(payload).send()
}
```

### Invoking contract: Transfering ETH 

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.provider.Request;

import java.math.BigInteger;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, Felt.ZERO);

        Felt recipientAccountAddress = Felt.fromHex("0x987654321");
        Uint256 amount = new Uint256(new Felt(451));

        // Specify the contract address, in this example ETH ERC20 contract is used
        Felt contractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");

        // Create a call
        List<Felt> calldata = List.of(recipientAccountAddress, amount.getLow(), amount.getHigh()); // amount is Uint256 and is represented by two Felt values
        Call call = new Call(contractAddress, "transfer", calldata);

        // Estimate fee for the invoke transaction
        Felt estimateFee = account.estimateFee(List.of(call)).send().get(0).getOverallFee();
        // Make sure to prefund the account with enough funds to cover the transaction fee and the amount to be transferred
        
        // Create and sign invoke transaction
        Request<InvokeFunctionResponse> request = account.execute(call);

        // Send the transaction
        InvokeFunctionResponse response = request.send();
    }
}
```
or in Kotlin

```kotlin
fun main(args: Array<String>) {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

    // Set up an account
    val privateKey = Felt.fromHex("0x123")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider)

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
    
    // Estimate fee for the invoke transaction
    val estimateFee = account.estimateFee(listOf(call)).send().first().overallFee
    // Make sure to prefund the account with enough funds to cover the transaction fee and the amount to be transferred
    
    // Create and sign invoke transaction
    val request = account.execute(call)
     
    // Send the transaction
    val response = request.send()
}
```

### Calling contract: Fetching ETH balance

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.math.BigInteger;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

        // Set up an account
        Felt privateKey = Felt.fromHex("0x123");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and key are for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, Felt.ZERO);

        // Specify the contract address, in this example ETH ERC20 contract is used
        Felt contractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");

        // Create a call
        List<Felt> calldata = List.of(account.getAddress());
        Call call = new Call(contractAddress, "balanceOf", calldata);
        Request<List<Felt>> request = provider.callContract(call);
        
        // Send the call request
        List<Felt> response = request.send();
        
        //Output value's type is Uint256 and is represented by two Felt values
        Uint256 balance = new Uint256(response.get(0), response.get(1));
    }
}
```
or in Kotlin

```kotlin
fun main(args: Array<String>) {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

    // Set up an account
    val privateKey = Felt.fromHex("0x123")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key have examples values for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider)

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

### Declaring Cairo 0 contract

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.transactions.DeclareTransactionV1Payload;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, Felt.ZERO);

        // Import a compiled contract
        Path contractPath = Paths.get("contract.json");
        String contractCode = String.join("", Files.readAllLines(contractPath));
        Cairo0ContractDefinition contractDefinition = new Cairo0ContractDefinition(contractCode);
        // Class hash is calculated using the tools you used for compilation (only for Cairo v0 contracts)
        Felt classHash = Felt.fromHex("0x1a3b2c");
        Felt nonce = account.getNonce().send();

        // Estimate fee for declaring a contract
        DeclareTransactionV1Payload declareTransactionPayloadForFeeEstimate = account.signDeclare(contractDefinition, classHash, new ExecutionParams(nonce, new Felt(1000000000000000L)), false);
        Request<List<EstimateFeeResponse>> feeEstimateRequest = provider.getEstimateFee(List.of(declareTransactionPayloadForFeeEstimate));
        Felt feeEstimate = feeEstimateRequest.send().get(0).getOverallFee();
        // Make sure to prefund the account with enough funds to cover the fee for declare transaction

        // Declare a contract
        Felt maxFee = new Felt(feeEstimate.getValue().multiply(BigInteger.TWO)); // Sometimes the actual fee may be higher than value from estimateFee
        ExecutionParams params = new ExecutionParams(nonce, maxFee);
        DeclareTransactionV1Payload declareTransactionPayload = account.signDeclare(contractDefinition, classHash, params, false);

        Request<DeclareResponse> request = provider.declareContract(declareTransactionPayload);
        DeclareResponse response = request.send();
    }
}
```
or in Kotlin

```kotlin
fun main(args: Array<String>) {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

    // Set up an account
    val privateKey = Felt.fromHex("0x123")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider)

    // Import a compiled contract
    val contractCode = Path.of("contract.json").readText()
    val contractDefinition = Cairo0ContractDefinition(contractCode)
    // Class hash is calculated using the tools you used for compilation (only for Cairo v0 contracts)
    val classHash = Felt.fromHex("0x1a3b2c")
    val nonce = account.getNonce().send()

    // Estimate fee for declaring a contract
    val declareTransactionPayloadForFeeEstimate = account.signDeclare(
        contractDefinition = contractDefinition,
        classHash = classHash,
        params = ExecutionParams(nonce, Felt(1000000000000000L)),
        forFeeEstimate = true,
    )
    val feeEstimateRequest = provider.getEstimateFee(listOf(declareTransactionPayloadForFeeEstimate))
    val feeEstimate = feeEstimateRequest.send().first().overallFee
    // Make sure to prefund the account with enough funds to cover the fee for declare transaction

    // Declare a contract
    val maxFee = Felt(feeEstimate.value.multiply(BigInteger.TWO)) // Sometimes the actual fee may be higher than value from estimateFee
    val params = ExecutionParams(
            nonce = nonce,
            maxFee = maxFee,
    )
    val declareTransactionPayload = account.signDeclare(
        contractDefinition = contractDefinition,
        classHash = classHash,
        params = params,
    )

    val request = provider.declareContract(declareTransactionPayload)
    val response = request.send()
}
```

### Declaring Cairo 1/2 contract

```java
import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.transactions.DeclareTransactionV2Payload;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Create a provider for interacting with Starknet
        Provider provider = new JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET);

        // Set up an account
        Felt privateKey = Felt.fromHex("0x1234");
        Felt accountAddress = Felt.fromHex("0x1236789");
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
        Account account = new StandardAccount(accountAddress, privateKey, provider, Felt.ZERO);

        // Import a compiled contract
        Path contractPath = Paths.get("contract.json");
        Path casmPath = Paths.get("contract.casm");
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));
        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);
        Felt nonce = account.getNonce().send();

        // Estimate fee for declaring a contract
        DeclareTransactionV2Payload declareTransactionPayloadForFeeEstimate = account.signDeclare(contractDefinition, casmContractDefinition, new ExecutionParams(nonce, new Felt(1000000000000000L)), false);
        Request<List<EstimateFeeResponse>> feeEstimateRequest = provider.getEstimateFee(List.of(declareTransactionPayloadForFeeEstimate));
        Felt feeEstimate = feeEstimateRequest.send().get(0).getOverallFee();
        // Make sure to prefund the account with enough funds to cover the fee for declare transaction

        // Declare a contract
        Felt maxFee = new Felt(feeEstimate.getValue().multiply(BigInteger.TWO)); // Sometimes the actual fee may be higher than value from estimateFee
        ExecutionParams params = new ExecutionParams(nonce, maxFee);
        DeclareTransactionV2Payload declareTransactionPayload = account.signDeclare(contractDefinition, casmContractDefinition, params, false);

        Request<DeclareResponse> request = provider.declareContract(declareTransactionPayload);
        DeclareResponse response = request.send();
    }
}
```
or in Kotlin

```kotlin
fun main(args: Array<String>) {
    // Create a provider for interacting with Starknet
    val provider = JsonRpcProvider("https://example-node-url.com/rpc", StarknetChainId.TESTNET)

    // Set up an account
    val privateKey = Felt.fromHex("0x1234")
    val accountAddress = Felt.fromHex("0x1236789")
    // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
    val account = StandardAccount(accountAddress, privateKey, provider)

    // Import a compiled contract
    val contractCode = Path.of("contract.json").readText()
    val casmCode = Path.of("contract.casm").readText()
    val contractDefinition = Cairo1ContractDefinition(contractCode)
    val casmContractDefinition = CasmContractDefinition(casmCode)
    val nonce = account.getNonce().send()

    // Estimate fee for declaring a contract
    val declareTransactionPayloadForFeeEstimate = account.signDeclare(
        sierraContractDefinition = contractDefinition,
        casmContractDefinition = casmContractDefinition,
        params = ExecutionParams(nonce, Felt(1000000000000000L)),
    )
    val feeEstimateRequest = provider.getEstimateFee(listOf(declareTransactionPayloadForFeeEstimate))
    val feeEstimate = feeEstimateRequest.send().first().overallFee
    // Make sure to prefund the account with enough funds to cover the fee for declare transaction

    // Declare a contract
    val maxFee = Felt(feeEstimate.value.multiply(BigInteger.TWO)) // Sometimes the actual fee may be higher than value from estimateFee
    val params = ExecutionParams(
            nonce = nonce,
            maxFee = maxFee,
    )
    val declareTransactionPayload = account.signDeclare(
        sierraContractDefinition = contractDefinition,
        casmContractDefinition = casmContractDefinition,
        params = params,
    )

    val request = provider.declareContract(declareTransactionPayload)
    val response = request.send()
}
```


## Reusing http clients

Make sure you don't create a new provider every time you want to use one. Instead, you should reuse existing instance.
This way you reuse connections and thread pools.

✅ **Do:** 
```java
var provider = GatewayProvider.makeTestnetClient();
var account1 = new StandardAccount(provider, accountAddress1, privateKey1);
var account2 = new StandardAccount(provider, accountAddress2, privateKey2);
```

❌ **Don't:**
```java
var provider1 = GatewayProvider.makeTestnetClient();
var account1 = new StandardAccount(provider1, accountAddress1, privateKey1);
var provider2 = GatewayProvider.makeTestnetClient();
var account2 = new StandardAccount(provider2, accountAddress2, privateKey2);
```


## Development

### Hooks
Run
```
./gradlew installKotlinterPrePushHook
```

## Running tests

### Prerequisites
- `cairo-lang` and `starknet-devnet`
  - These are distributed as python packages. To install, run:
    ```shell
    pip install -r requirements.txt
    ```
- [`starknet-devnet-rs`](https://github.com/0xSpaceShard/starknet-devnet-rs) 
  - Since it has yet to be released, you will need to build it manually and set `DEVNET_PATH` environment variable that points to a binary:
    ```shell
    DEVNET_PATH=/path/to/starknet-devnet-rs/target/release/starknet-devnet
    ```
  - You can do so by using environment variables in your system or IDE, or by sourcing an `.env` file. Refer to the example config found in [test_variables.env.example](test_variables.env.example).
- [`starknet-foundry`](https://github.com/foundry-rs/starknet-foundry) - provides `sncast` cli
- [`asdf`](https://github.com/asdf-vm/asdf) version manager and [`asdf scarb`](https://github.com/software-mansion/asdf-scarb) plugin

### Platform-specific prerequisites
- **macOS aarch64**: no additional steps are required.
- **linux x86_64**: no additional steps are required.
- For other platforms, you will need to set `V2_COMPILER_BUILD_PATH` environment variable.
    - `V2_COMPILER_BUILD_PATH` - path to a directory that contains built cairo v2.2.0 compiler binaries (`bin/`) and corelib (`corelib/`)
    - To build cairo compilers for your platform, refer to [cairo repo](https://github.com/starkware-libs/cairo).

### Regular Tests
Use the following command to run tests:
```shell
./gradlew :lib:test
```

### Integration Tests
Running tests for integration network requires a valid configuration. It can be set using environment variables in your system or IDE, or by sourcing an `.env` file. 
Refer to the example config found in [test_variables.env.example](test_variables.env.example).
Please note that while there are publicly accessible gateway URLs, you will additionally need a `RPC node URL` and an `account address` (along with its `private key`), to run these tests.

Integration tests are disabled by default. To enable them, you can set the env variable: 
```env
INTEGRATION_TEST_MODE=non_gas
```
Some of integration tests require gas and are disabled by default. If you want to run them as well, you can set:
```env 
INTEGRATION_TEST_MODE=all
```
⚠️ WARNING ⚠️ Please be aware that in that case your integration account address must have a pre-existing balance as these tests will consume some funds.

Alternatively, you can use flag to specify whether to run integration and gas tests:
```shell
./gradlew :lib:test -PintegrationTestMode=non_gas
./gradlew :lib:test -PintegrationTestMode=all
```
Flag takes precendece over the env variable if both are set.

⚠️ WARNING ⚠️ Some integration tests may fail due to getNonce receiving higher nonce than expected by other methods.
It is adviced to additionaly provide an account (along with its `private key`) with a constant `nonce` to ensure non-gas tests pass.
Such account shouldn't be used for any other purpose than running non-gas integration tests.
If not set, the main integration account will be used for this purpose.

### Ensuring idiomatic Java code
We want this library to be used by both kotlin & java users. In order to ensure a nice API for java always follow those rules: 
1. When using file level functions use `@file:JvmName(NAME)` to ensure a nice name without `Kt` suffix.
2. When using a companion object mark every property/function with `@JvmStatic`. This way they are accessible as static from the class. Without it `Class.INSTANCE` would have to be used.
3. When defining an immutable constant use `@field:JvmField`. This makes them static properties without getters/setters in java.
4. If you are not sure how something would work in java just create a new java class, import your code and check yourself.
5. Avoid using default arguments. It is better to overload a function and specify defaults there.


## Building documentation

Documentation is written in Kdoc format and markdown and is generated using `Dokka`. Execute
following commands from `/lib` to build docs.

* `./gradlew dokkaHtml` to build kotlin format docs
* `./gradlew dokkaHtmlJava` to build java format docs

Generated documentation can be found in their respective folders inside `/build/dokka`.
