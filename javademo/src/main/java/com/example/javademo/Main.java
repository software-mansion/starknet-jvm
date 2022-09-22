package com.example.javademo;

import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.*;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Main {
    public static void main(String[] args) throws Exception {
        // Set up an account
        Felt address = Felt.fromHex("0x1234");
        Felt privateKey = new Felt(new Random().nextLong(1, Long.MAX_VALUE));
        Provider provider = GatewayProvider.makeTestnetClient();
        Account account = new StandardAccount(address, privateKey, provider);

        // Read contract source code
        Path contractPath = Paths.get("balance.cairo");
        String contract = String.join("", Files.readAllLines(contractPath));

        // Deploy a contract
        ContractDefinition contractDefinition = new ContractDefinition(contract);
        DeployTransactionPayload payload = new DeployTransactionPayload(contractDefinition, Felt.fromHex("0x1234"), Collections.emptyList(), Felt.ZERO);
        Request<DeployResponse> deployRequest = provider.deployContract(payload);
        DeployResponse deployResponse = deployRequest.send();

        // Invoke a contract
        Felt contractAddress = deployResponse.getContractAddress();
        Call call = new Call(contractAddress, "increaseBalance", List.of(new Felt(1000)));
        Request<InvokeFunctionResponse> executeRequest = account.execute(call);
        InvokeFunctionResponse executeResponse = executeRequest.send();

        // Make sure that the transaction succeeded
        Request<? extends TransactionReceipt> receiptRequest = provider.getTransactionReceipt(executeResponse.getTransactionHash());
        TransactionReceipt receipt = receiptRequest.send();
        Boolean isAccepted = (receipt.getStatus() == TransactionStatus.ACCEPTED_ON_L2) || (receipt.getStatus() == TransactionStatus.ACCEPTED_ON_L1);
    }
}
