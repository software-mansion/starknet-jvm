package com.example.javademo;

import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.crypto.StarknetCurveSignature;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.DeclareTransactionPayload;
import com.swmansion.starknet.data.types.transactions.DeclareTransactionV1Payload;
import com.swmansion.starknet.data.types.transactions.DeclareTransactionV2Payload;
import com.swmansion.starknet.data.types.transactions.TransactionReceipt;
import com.swmansion.starknet.deployercontract.ContractDeployment;
import com.swmansion.starknet.deployercontract.StandardDeployer;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.rpc.JsonRpcProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;


public class Main {
    private static class DemoConfig {
        // Please set the environment variables or replace with actual values manually
        public static String rpcNodeUrl = System.getenv().getOrDefault("DEMO_RPC_URL", "https://example-node-url.com/rpc");
        public static String accountAddress = System.getenv().getOrDefault("DEMO_ACCOUNT_ADDRESS", "0x123456");
        public static String accountPrivateKey = System.getenv().getOrDefault("DEMO_PRIVATE_KEY", "0x789");
    }

    public static void main(String[] args) throws Exception {
        // Create a provider for interacting with Starknet
        JsonRpcProvider provider = new JsonRpcProvider(DemoConfig.rpcNodeUrl, StarknetChainId.TESTNET);

        // Set up an account
        // Please note the account must be deployed and have enough funds to paying the fees
        // ⚠️ WARNING ⚠️ Both the account address and private key are for demonstration purposes only.
        Felt address = Felt.fromHex(DemoConfig.accountAddress);
        Felt privateKey = Felt.fromHex(DemoConfig.accountPrivateKey);
        // Make sure to check Cairo version of account contract
        Account account = new StandardAccount(address, privateKey, provider, Felt.ZERO);

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

        Request<InvokeFunctionResponse> executeRequest = account.execute(invokeCall);
        InvokeFunctionResponse executeResponse = executeRequest.send();
        Thread.sleep(10000); // Wait for invoke tx to complete

        // Make sure that the transaction succeeded
        Request<? extends TransactionReceipt> receiptRequest = provider.getTransactionReceipt(executeResponse.getTransactionHash());
        TransactionReceipt receipt = receiptRequest.send();
        Boolean isAccepted = receipt.isAccepted();
        System.out.println(isAccepted);

        // Call contract (Get ETH balance)
        Call call = new Call(erc20ContractAddress, "balanceOf", List.of(account.getAddress()));

        Request<List<Felt>> callRequest = provider.callContract(call);
        List<Felt> callResponse = callRequest.send();
        // Output value's type is UInt256 and is represented by two Felt values
        Uint256 balance = new Uint256(callResponse.get(0), callResponse.get(1));
        System.out.println(balance);

        // Declare Cairo 0 contract
        // Aside from contract code, you will additionaly need to provide classHash of said contract
        // Class hash is calculated using the tools you used for compilation

        Felt cairo0ContractClassHash = Felt.fromHex("0x3b32bb615844ea7a9a56a8966af1a5ba1457b1f5c9162927ca1968975b0d2a9");
        Path cairo0ContractPath = Paths.get("javademo/src/main/resources/contracts_v0/target/release/balance.json");
        DeclareResponse cairo0DeclareResponse = declareCairo0Contract(account, provider, cairo0ContractPath, cairo0ContractClassHash);

        // TODO: (#336) re-enable once loading posedion from jar is fixed
        // Declare Cairo 1 contract
        // You need to provide both sierra and casm codes of compiled contracts
        // Path sierraPath = Paths.get("javademo/src/main/resources/contracts/target/release/demo_Balance.sierra.json");
        // Path casmPath = Paths.get("javademo/src/main/resources/contracts/target/release/demo_Balance.casm.json");
        // DeclareResponse cairo1DeclareResponse = declareCairo1Contract(account, provider, sierraPath, casmPath);
        // Felt cairo1ContractClassHash = cairo1DeclareResponse.getClassHash();
        // Thread.sleep(15000); // Wait for declare tx to complete

        // Deploy a contract with Universal Deployer Contract (for both cairo 1 and cairo 0 contracts)
        DeployContractResult deployContractResult = deployContract(account, provider, cairo0ContractClassHash, Collections.emptyList());
        Felt deployedContractAddress = deployContractResult.contractAddress;
        Thread.sleep(15000); // Wait for deploy tx to complete

        // Manually sign a hash
        Felt hash = Felt.fromHex("0x121212121212");
        StarknetCurveSignature signature = StarknetCurve.sign(privateKey, hash);
        Felt r = signature.getR();
        Felt s = signature.getS();

        // Get a public key for a given private key
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);

        // Verify a signature
        boolean isCorrect = StarknetCurve.verify(publicKey, hash, r, s);
        System.out.println(isCorrect);
    }

    private static DeclareResponse declareCairo0Contract(Account account, Provider provider, Path contractPath, Felt classHash) throws IOException {
        // Read contract source code
        String contractCode = String.join("", Files.readAllLines(contractPath));
        Cairo0ContractDefinition contractDefinition = new Cairo0ContractDefinition(contractCode);

        Felt nonce = account.getNonce().send();

        // Estimate fee for declaring a contract
        DeclareTransactionPayload declareTransactionPayloadForFeeEstimate = account.signDeclare(contractDefinition, classHash, new ExecutionParams(nonce, new Felt(1000000000000000L)), false);
        Request<List<EstimateFeeResponse>> feeEstimateRequest = provider.getEstimateFee(List.of(declareTransactionPayloadForFeeEstimate));
        Felt feeEstimate = feeEstimateRequest.send().get(0).getOverallFee();
        Felt maxFee = new Felt(feeEstimate.getValue().multiply(BigInteger.TWO));
        // Make sure to prefund the account with enough funds to cover the fee for declare transaction

        ExecutionParams executionParams = new ExecutionParams(nonce, maxFee);
        DeclareTransactionV1Payload payload = account.signDeclare(contractDefinition, classHash, executionParams, false);
        Request<DeclareResponse> request = provider.declareContract(payload);

        return request.send();
    }

    private static DeclareResponse declareCairo1Contract(Account account, Provider provider, Path contractPath, Path casmPath) throws IOException {
        // Read contract source code
        String contractCode = String.join("", Files.readAllLines(contractPath));
        String casmCode = String.join("", Files.readAllLines(casmPath));
        Cairo1ContractDefinition contractDefinition = new Cairo1ContractDefinition(contractCode);
        CasmContractDefinition casmContractDefinition = new CasmContractDefinition(casmCode);
        // Class hash is calculated using the tools you used for compilation (only for Cairo v0 contracts)
        Felt nonce = account.getNonce().send();

        // Estimate fee for declaring a contract
        DeclareTransactionV2Payload declareTransactionPayloadForFeeEstimate = account.signDeclare(contractDefinition, casmContractDefinition, new ExecutionParams(nonce, new Felt(1000000000000000L)), false);
        Request<List<EstimateFeeResponse>> feeEstimateRequest = provider.getEstimateFee(List.of(declareTransactionPayloadForFeeEstimate));
        Felt feeEstimate = feeEstimateRequest.send().get(0).getOverallFee();
        // Make sure to prefund the account with enough funds to cover the fee for declare transaction

        // Declare a contract
        ExecutionParams params = new ExecutionParams(nonce, new Felt(feeEstimate.getValue().multiply(BigInteger.TEN)));
        DeclareTransactionV2Payload declareTransactionPayload = account.signDeclare(contractDefinition, casmContractDefinition, params, false);

        Request<DeclareResponse> request = provider.declareContract(declareTransactionPayload);

        return request.send();
    }

    private static DeployContractResult deployContract(Account account, Provider provider, Felt classHash, List<Felt> constructorCalldata) {
        Felt udcTestnetAddress = Felt.fromHex("0x041a78e741e5af2fec34b695679bc6891742439f7afb8484ecd7766661ad02bf");
        Felt salt = new Felt(20);
        StandardDeployer contractDeployer = new StandardDeployer(udcTestnetAddress, provider, account);
        ContractDeployment deployResponse = contractDeployer.deployContract(classHash, true, salt, Collections.emptyList()).send();

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
