#!/bin/sh

#cd ./lib/src/test/resources
cd $(dirname $0)

starknet-compile testContract.cairo --output testContract.json --abi testContractAbi.json

starknet deploy --contract testContract.json --salt 0x0