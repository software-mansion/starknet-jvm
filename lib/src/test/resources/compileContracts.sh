#!/bin/bash

readonly V1_CONTRACT_PATH="contracts_v1"
readonly V2_CONTRACT_PATH="contracts_v2"
readonly V2_6_CONTRACT_PATH="contracts_v2_6"

readonly V1_SCARB_VERSION="0.4.0"
readonly V2_SCARB_VERSION="0.7.0"
readonly V2_6_SCARB_VERSION="2.6.0"


echo "Compiling v1 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V1_CONTRACT_PATH" || exit 1
asdf local scarb $V1_SCARB_VERSION || exit 1
scarb --profile release build
popd || exit 1
popd || exit 1
echo "Done!"


echo "Compiling v2 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V2_CONTRACT_PATH" || exit 1
asdf local scarb $V2_SCARB_VERSION || exit 1
scarb --profile release build
popd || exit 1
popd || exit 1
echo "Done!"

echo "Compiling v2.6 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V2_6_CONTRACT_PATH" || exit 1
asdf local scarb $V2_6_SCARB_VERSION || exit 1
scarb --profile release build
popd || exit 1
popd || exit 1
echo "Done!"
