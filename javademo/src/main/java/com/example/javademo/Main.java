package com.example.javademo;

import com.swmansion.starknet.crypto.StarknetCurve;
import com.swmansion.starknet.data.types.Felt;

public class Main {
    public static void main(String[] args) {
        var result = StarknetCurve.pedersen(new Felt(1), new Felt(2));
        System.out.println("Pedersen result: " + result);
    }
}
