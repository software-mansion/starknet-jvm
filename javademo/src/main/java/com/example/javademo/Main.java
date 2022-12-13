package com.example.javademo;

import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.crypto.StarknetCurveSignature;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.*;
import com.swmansion.starknet.deployercontract.ContractDeployment;
import com.swmansion.starknet.deployercontract.StandardDeployer;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Collections.emptyList;

public class Main {
    public static void main(String[] args) throws Exception {
        // Set up an account
        Felt address = Felt.fromHex("0x1234");
        // ⚠️ WARNING ⚠️ The key generated here is just for demonstration purposes.
        // DO NOT GENERATE YOUR KEYS THIS WAY. USE CRYPTOGRAPHICALLY SAFE TOOLS!
        Felt privateKey = new Felt(ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        Provider provider = GatewayProvider.makeTestnetClient();
        Account account = new StandardAccount(address, privateKey, provider);

        // Read contract source code
        Path contractPath = Paths.get("balance.cairo");
        String contract = String.join("", Files.readAllLines(contractPath));

        // Declare a contract
        // Class hash has to be calculated manually
        ContractDefinition contractDefinition = new ContractDefinition(contract);
        Felt classHash = Felt.fromHex("0x399998c787e0a063c3ac1d2abac084dcbe09954e3b156d53a8c43a02aa27d35");
        Felt maxFee = Felt.ZERO;
        Felt nonce = account.getNonce().send();
        ExecutionParams executionParams = new ExecutionParams(nonce, maxFee);
        DeclareTransactionPayload declareTransactionPayload = account.signDeclare(contractDefinition, classHash, executionParams);
        DeclareResponse declareResponse = provider.declareContract(declareTransactionPayload).send();

        // Deploy a contract with Universal Deployer Contract
        StandardDeployer contractDeployer = new StandardDeployer(Felt.fromHex("0x041a78e741e5af2fec34b695679bc6891742439f7afb8484ecd7766661ad02bf"), provider, account);
        ContractDeployment deployResponse = contractDeployer.deployContract(classHash, true, Felt.fromHex("0x12345678"), emptyList()).send();

        // Invoke a contract
        Felt contractAddress = contractDeployer.findContractAddress(deployResponse).send();
        Call call = new Call(contractAddress, "increaseBalance", List.of(new Felt(1000)));
        // Or using any objects implementing ConvertibleToCalldata interface
        Call callFromCallArguments = Call.fromCallArguments(
                contractAddress,
                "increaseBalance",
                List.of(Uint256.fromHex("0x9148582852675472"), new Felt(1000))
        );

        // Invoke a contract
        Request<InvokeFunctionResponse> executeRequest = account.execute(call);
        InvokeFunctionResponse executeResponse = executeRequest.send();

        // Make sure that the transaction succeeded
        Request<? extends TransactionReceipt> receiptRequest = provider.getTransactionReceipt(executeResponse.getTransactionHash());
        TransactionReceipt receipt = receiptRequest.send();
        Boolean isAccepted = receipt.isAccepted();

        // Manually sign a hash
        Felt hash = Felt.fromHex("0x121212121212");
        StarknetCurveSignature signature = StarknetCurve.sign(privateKey, hash);
        Felt r = signature.getR();
        Felt s = signature.getS();

        // Get a public key for a given private key
        Felt publicKey = StarknetCurve.getPublicKey(privateKey);

        // Verify a signature
        boolean isCorrect = StarknetCurve.verify(publicKey, hash, r, s);
    }
}
