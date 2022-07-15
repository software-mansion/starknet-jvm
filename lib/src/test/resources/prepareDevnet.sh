#!/bin/sh

#cd ./lib/src/test/resources
cd $(dirname $0)

starknet-compile testContract.cairo --output testContract.json --abi testContractAbi.json

starknet deploy --gateway_url "http://127.0.0.1:5050/gateway" --feeder_gateway_url "http://127.0.0.1:5050/feeder_gateway"  --contract testContract.json --salt 0x0
