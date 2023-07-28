#!/bin/bash

pushd "$(dirname "$0")" || exit

mkdir -p "compiled_v0"

echo "Compiling v0 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  if [[ $name == *"account"* ]]; then
    starknet-compile-deprecated "$file" --account_contract --output "compiled_v0/$name.json" --abi "compiled_v0/${name}Abi.json"
  else
    starknet-compile-deprecated "$file" --output "compiled_v0/$name.json" --abi "compiled_v0/${name}Abi.json"
  fi
done < <(find src_v0 -name "*.cairo" -type f -print0)
popd
echo "Done!"

if ! which cargo > /dev/null; then
  echo "Installing rust..."
  curl https://sh.rustup.rs -sSf | sh -s -- -y
fi

if ! find ../cairo/target/debug -name starknet-compile -type f > /dev/null || ! find ../cairo/target/debug -name starknet-sierra-compile -type f > /dev/null; then
  echo "Building starknet compiler"
  pushd ../cairo || exit 1
  cargo build
  cargo run --bin starknet-compile -- --version
  cargo run --bin starknet-sierra-compile -- --version
  popd
fi

COMPILER_PATH=$(pwd)/../cairo/target/debug/starknet-compile
SIERRA_PATH=$(pwd)/../cairo/target/debug/starknet-sierra-compile

pushd "$(dirname "$0")" || exit 1
mkdir -p "compiled_v1"

echo "Compiling v1 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH --allowed-libfuncs-list-name experimental_v0.1.0 "$file" "compiled_v1/$name.json"
  $SIERRA_PATH --allowed-libfuncs-list-name experimental_v0.1.0 --add-pythonic-hints "compiled_v1/$name.json" "compiled_v1/$name.casm"
done < <(find src_v1 -name "*.cairo" -type f -print0)
popd
echo "Done!"
