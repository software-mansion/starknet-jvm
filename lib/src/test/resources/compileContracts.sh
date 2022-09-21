#!/bin/bash

cd "$(dirname "$0")" || exit
mkdir -p "compiled"

# shellcheck disable=SC2044
for file in $(find src -name "*.cairo" -type f -print); do

  name="$(basename -- "$file" .cairo)"

  if [[ $name == *"account"* ]]; then
    starknet-compile "$file" --account_contract --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
  else
    starknet-compile "$file" --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
  fi
done
