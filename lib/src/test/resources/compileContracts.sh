#!/bin/sh

cd "$(dirname $0)" || exit

for file in src/*.cairo; do
  [ -f "$file" ] || break

  name="$(basename -- "$file" .cairo)"

  starknet-compile "$file" --output "compiled/$name.json" --abi "compiled/${name}Abi.json"
done
