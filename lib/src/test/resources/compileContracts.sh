#!/bin/bash

cd "$(dirname "$0")" || exit
mkdir -p "compiled"

for file in src/*.cairo; do
  [ -f "$file" ] || break

  name="$(basename -- "$file" .cairo)"

  if [[ $name == *"account"* ]]; then
    starknet-compile "$file" --account_contract --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
  else
    starknet-compile "$file" --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
  fi
done
