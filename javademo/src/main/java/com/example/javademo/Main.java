package com.example.javademo;

import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.crypto.StarknetCurveSignature;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.deployercontract.ContractDeployment;
import com.swmansion.starknet.deployercontract.Deployer;
import com.swmansion.starknet.deployercontract.StandardDeployer;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;
import com.swmansion.starknet.service.http.requests.HttpBatchRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.swmansion.starknet.data.Selector.selectorFromName;


public class Main {
    private enum DemoProfile {NETWORK, DEVNET}

    private static class DemoConfig {
        // Make sure to run starknet-devnet-rs according to the instructions in README.md
        // Alternatively, you can modify these values to use demo on a network other than devnet
        public static final DemoProfile profile = DemoProfile.DEVNET;
        public static final String rpcNodeUrl = "http://127.0.0.1:5050/rpc";
        public static final String accountAddress = "0x1323cacbc02b4aaed9bb6b24d121fb712d8946376040990f2f2fa0dcf17bb5b";
        public static final String accountPrivateKey = "0xa2ed22bb0cb0b49c69f6d6a8d24bc5ea";
    }

    public static void main(String[] args) throws Exception {
        // Create a provider for interacting with Starknet
        JsonRpcProvider provider = new JsonRpcProvider(DemoConfig.rpcNodeUrl);

        // Set up an account
        // Please note the account must be deployed and have enough funds to paying the fees
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
        Felt address = Felt.fromHex(DemoConfig.accountAddress);
        Felt privateKey = Felt.fromHex(DemoConfig.accountPrivateKey);
        // Make sure to check Cairo version of account contract
        StarknetChainId chainId = provider.getChainId().send();
        Account account = new StandardAccount(address, privateKey, provider, chainId);

        // Invoke a contract (Transfer ETH)
        Felt recipientAccountAddress = Felt.fromHex("0x987654321");
        Uint256 amount = new Uint256(new Felt(451));
        // Specify the contract address, in this example ERC-20 ETH contract is used
        Felt erc20ContractAddress = Felt.fromHex("0x049d36570d4e46f48e99674bd3fcc84644ddd6b96f7c741b1562b82f9e004dc7");
        // Create a call
        List<Felt> calldata = List.of(recipientAccountAddress, amount.getLow(), amount.getHigh());
        Call invokeCall = new Call(erc20ContractAddress, "transfer", calldata);
        // Or using any objects implementing ConvertibleToCalldata interface
        Call callFromCallArguments = Call.fromCallArguments(
                erc20ContractAddress,
                "transfer",
                List.of(erc20ContractAddress, amount)
        );

        Request<InvokeFunctionResponse> executeRequest = account.executeV3(invokeCall);
        InvokeFunctionResponse executeResponse = executeRequest.send();

        if (DemoConfig.profile == DemoProfile.NETWORK) {
            Thread.sleep(20000); // wait for invoke tx to complete
        }

        // Make sure that the transaction succeeded
        Request<? extends TransactionReceipt> invokeReceiptRequest = provider.getTransactionReceipt(executeResponse.getTransactionHash());
        TransactionReceipt invokeReceipt = invokeReceiptRequest.send();

        System.out.println("Was invoke transaction accepted? " + invokeReceipt.isAccepted() + ".");

        // Call contract (Get ETH balance)
        Call call = new Call(erc20ContractAddress, "balanceOf", List.of(account.getAddress()));

        Request<FeltArray> callRequest = provider.callContract(call);
        List<Felt> callResponse = callRequest.send();
        // Output value's type is UInt256 and is represented by two Felt values
        Uint256 balance = new Uint256(callResponse.get(0), callResponse.get(1));
        System.out.println("Balance: " + balance.getValue() + " wei.");

        // Declare Cairo 1 contract
        // You need to provide both sierra and casm codes of compiled contracts
        Path sierraPath = Paths.get("src/main/resources/contracts/target/release/demo_Balance.sierra.json");
        Path casmPath = Paths.get("src/main/resources/contracts/target/release/demo_Balance.casm.json");

        DeclareResponse declareResponse = declareCairo1Contract(account, provider, sierraPath, casmPath);
        Felt contractClassHash = declareResponse.getClassHash();

        if (DemoConfig.profile == DemoProfile.NETWORK) {
            Thread.sleep(60000); // wait for declare tx to complete
        }

        Request<? extends TransactionReceipt> declareReceiptRequest = provider.getTransactionReceipt(declareResponse.getTransactionHash());
        TransactionReceipt declareReceipt = declareReceiptRequest.send();
        System.out.println("Was declare v3 transaction accepted? " + declareReceipt.isAccepted() + ".");

        // Deploy a contract with Universal Deployer Contract
        Felt initialBalance = new Felt(500);
        List<Felt> constructorCalldata = List.of(initialBalance);
        DeployContractResult deployContractResult = deployContract(account, provider, contractClassHash, constructorCalldata);
        Felt deployedContractAddress = deployContractResult.contractAddress;

        if (DemoConfig.profile == DemoProfile.NETWORK) {
            Thread.sleep(60000); // wait for deploy tx to complete
        }

        Request<? extends TransactionReceipt> deployReceiptRequest = provider.getTransactionReceipt(deployContractResult.transactionHash);
        TransactionReceipt deployReceipt = deployReceiptRequest.send();

        System.out.println("Was deploy transaction accepted? " + deployReceipt.isAccepted() + ".");
        System.out.println("Deployed contract address: " + deployedContractAddress + ".");

        // Check the initial value of `balance` in deployed contract
        Request<Felt> getStorageRequest = provider.getStorageAt(deployedContractAddress, selectorFromName("balance"));
        Felt initialContractBalance = getStorageRequest.send();
        System.out.println("Initial contract balance: " + initialContractBalance.getValue() + ".");

        // Manually sign a hash
        Felt hash = Felt.fromHex("0x121212121212");
        StarknetCurveSignature signature = StarknetCurve.sign(privateKey, hash);
        Felt r = signature.getR();
        Felt s = signature.getS();

        // Get a public key for a given private key
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);

        // Verify a signature
        boolean isCorrect = StarknetCurve.verify(publicKey, hash, r, s);
        System.out.println("Is signature correct? " + isCorrect + ".");

        // Batch RPC requests
        // Get transaction statuses
        HttpBatchRequest<GetTransactionStatusResponse> statusesRequest = provider.batchRequests(
                provider.getTransactionStatus(declareResponse.getTransactionHash()),
                provider.getTransactionStatus(deployContractResult.transactionHash)
        );

        List<RequestResult<GetTransactionStatusResponse>> statusesResponse = statusesRequest.send();

        GetTransactionStatusResponse declareTransactionStatus = statusesResponse.get(0).getOrNull();
        GetTransactionStatusResponse deployContractTransactionStatus = statusesResponse.get(1).getOrNull();

        System.out.println("Declare transaction execution status: " + declareTransactionStatus.getExecutionStatus());
        System.out.println("Deploy transaction contract execution status: " + deployContractTransactionStatus.getExecutionStatus());

        // Batch any RPC requests
        // Get block hash and number + Check the initial value of `balance` in deployed contract
        HttpBatchRequest mixedRequest = provider.batchRequestsAny(
                provider.getBlockHashAndNumber(),
                provider.getStorageAt(deployedContractAddress, selectorFromName("balance"))
        );

        List<RequestResult> mixedResponse = mixedRequest.send();

        GetBlockHashAndNumberResponse blockHashAndNumber = (GetBlockHashAndNumberResponse) mixedResponse.get(0).getOrNull();
        Felt initialContractBalance2 = (Felt) mixedResponse.get(1).getOrNull();

        System.out.println("Block hash: " + blockHashAndNumber.getBlockHash() + ".");
        System.out.println("Initial contract balance: " + initialContractBalance2.getValue() + ".");

    }

    private static DeclareResponse declareCairo1Contract(Account account, Provider provider, Path contractPath, Path casmPath) throws IOException {
        // Read contract source code
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));
        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);

        // Get nonce of the account used for declaring a contract
        Felt nonce = account.getNonce().send();

        // Estimate fee for declaring a contract
        DeclareTransactionV3 declareTransactionPayloadForFeeEstimate = account.signDeclareV3(contractDefinition, casmContractDefinition, new DeclareParamsV3(nonce, ResourceBounds.ZERO), true);
        Request<EstimateFeeResponseList> feeEstimateRequest = provider.getEstimateFee(List.of(declareTransactionPayloadForFeeEstimate));
        EstimateFeeResponse feeEstimate = feeEstimateRequest.send().getValues().get(0);
        // Make sure to prefund the account with enough funds to cover the fee for declare transaction

        // Declare a contract
        ResourceBounds l1ResourceBounds = feeEstimate.toResourceBounds(1.5, 1.5).getL1Gas();
        DeclareParamsV3 params = new DeclareParamsV3(nonce, l1ResourceBounds);
        DeclareTransactionV3 declareTransactionPayload = account.signDeclareV3(contractDefinition, casmContractDefinition, params, false);

        Request<DeclareResponse> request = provider.declareContract(declareTransactionPayload);

        return request.send();
    }

    private static DeployContractResult deployContract(Account account, Provider provider, Felt classHash, List<Felt> constructorCalldata) {
        // The address of Universal Deployer Contract
        Felt udcAddress = Felt.fromHex("0x041a78e741e5af2fec34b695679bc6891742439f7afb8484ecd7766661ad02bf");
        Felt salt = new Felt(20);

        // Deploy a contract
        Deployer contractDeployer = new StandardDeployer(udcAddress, provider, account);
        Request<ContractDeployment> deployRequest = contractDeployer.deployContractV3(classHash, true, salt, constructorCalldata);
        ContractDeployment deployResponse = deployRequest.send();

        // Find the address of deployed contract
        Felt contractAddress = contractDeployer.findContractAddress(deployResponse).send();
        return new DeployContractResult(deployResponse.getTransactionHash(), contractAddress);
    }

    private static class DeployContractResult {
        public Felt transactionHash;
        public Felt contractAddress;

        public DeployContractResult(Felt transactionHash, Felt contractAddress) {
            this.transactionHash = transactionHash;
            this.contractAddress = contractAddress;
        }
    }
}
