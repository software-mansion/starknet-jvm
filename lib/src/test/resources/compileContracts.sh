#!/bin/bash

cd "$(dirname "$0")" || exit
mkdir -p "compiled"

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  if [[ $name == *"account"* ]]; then
    starknet-compile-deprecated "$file" --account_contract --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
  else
    starknet-compile-deprecated "$file" --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
  fi
done < <(find src -name "*.cairo" -type f -print0)
