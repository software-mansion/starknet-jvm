#!/bin/bash

readonly V0_CONTRACT_PATH="contracts_v0"
readonly V1_CONTRACT_PATH="contracts_v1"
readonly V2_CONTRACT_PATH="contracts_v2"

readonly V0_ARTIFACT_PATH="contracts_v0/target/release"

readonly V1_SCARB_VERSION="0.4.0"
readonly V2_SCARB_VERSION="0.7.0"

echo "Installing scarb..."
asdf plugin add scarb || true
echo "Done!"

pushd "$(dirname "$0")" || exit 1
mkdir -p "$V0_ARTIFACT_PATH"
echo "Compiling v0 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  if [[ $name == *"account"* ]]; then
    starknet-compile-deprecated "$file" --account_contract --output "$V0_ARTIFACT_PATH/$name.json" --abi "$V0_ARTIFACT_PATH/${name}Abi.json" || exit 1
  else
    starknet-compile-deprecated "$file" --output "$V0_ARTIFACT_PATH/$name.json" --abi "$V0_ARTIFACT_PATH/${name}Abi.json" || exit 1
  fi
done < <(find "$V0_CONTRACT_PATH" -name "*.cairo" -type f -print0)
popd || exit 1
echo "Done!"


echo "Compiling v1 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V1_CONTRACT_PATH" || exit 1
asdf install scarb $V1_SCARB_VERSION || true
asdf local scarb $V1_SCARB_VERSION || exit 1
scarb --profile release build
popd || exit 1
popd || exit 1
echo "Done!"


echo "Compiling v2 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V2_CONTRACT_PATH" || exit 1
asdf install scarb $V2_SCARB_VERSION || true
asdf local scarb $V2_SCARB_VERSION || exit 1
scarb --profile release build
popd || exit 1
popd || exit 1
echo "Done!"
