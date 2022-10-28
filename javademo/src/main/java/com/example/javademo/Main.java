package com.example.javademo;

import com.swmansion.starknet.account.Account;
import com.swmansion.starknet.account.StandardAccount;
import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.crypto.StarknetCurveSignature;
import com.swmansion.starknet.data.types.*;
import com.swmansion.starknet.data.types.transactions.DeployTransactionPayload;
import com.swmansion.starknet.data.types.transactions.TransactionReceipt;
import com.swmansion.starknet.data.types.transactions.TransactionStatus;
import com.swmansion.starknet.provider.Provider;
import com.swmansion.starknet.provider.Request;
import com.swmansion.starknet.provider.gateway.GatewayProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    public static void main(String[] args) throws Exception {
        Felt hash = Felt.fromHex("0x121212121212");
        StarknetCurveSignature signature = StarknetCurve.sign(hash, hash);
        Felt r = signature.getR();
        Felt s = signature.getS();

        var client = GatewayProvider.makeTestnetClient();
        var address2 = client.callContract(
                new Call(
                        Felt.fromHex(
                                "0x07ce3c2bf1b9362146e214756c9e121e21bf4225ef7252fa93ff7a7dd1f67a55"
                        ),
                        "get_implementation",
                        List.of()
                ), BlockTag.LATEST).send();
        System.out.println("address");
        System.out.println(address2);
    }
}
