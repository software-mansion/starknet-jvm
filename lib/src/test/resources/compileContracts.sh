#!/bin/bash
# #!/opt/homebrew/bin/bash

get_release_url() {
  local VERSION=$1

  OS=$(uname)
  ARCH=$(uname -m)

  local OS_DLOAD="unknown-os"
  local ARCH_DLOAD="unknown-Aarch"
  local EXT="tar.gz"

  if [ "$OS" == "Linux" ]; then
    OS_DLOAD="unknown-linux-musl"
    ARCH_DLOAD="x86_64"
    EXT="tar.gz"
  elif [ "$OS" == "Darwin" ]; then
    OS_DLOAD="apple-darwin"
    ARCH_DLOAD="aarch64"
    EXT="tar"
  fi

#  if [ "$ARCH" == "x86_64" ]; then
#    ARCH_DLOAD="x86_64"
#  elif [ "$ARCH" == "arm64" ]; then
#    ARCH_DLOAD="aarch64"
#  fi

  echo "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-${ARCH_DLOAD}-${OS_DLOAD}.${EXT}"
}

fetch_compilers() {
  local VERSION=$1
  local FOLDER=$2
  local COMPILER_URL=$(get_release_url "$VERSION")

  echo "Fetching compiler binaries for Cairo $VERSION..."
  echo "URL: $COMPILER_URL"
  mkdir -p "$FOLDER"
  curl -LsSf "$COMPILER_URL" | tar -xz -C "$FOLDER"

  COMPILER_PATH=$(pwd)/"$FOLDER"/cairo/bin/starknet-compile
  SIERRA_PATH=$(pwd)/"$FOLDER"/cairo/bin/starknet-sierra-compile

  echo "Done!"
}

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

pushd "$(dirname "$0")" || exit 1
fetch_compilers "1.1.1" "cairo_compilers_v1"
mkdir -p "compiled_v1"

echo "Compiling v1 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH --allowed-libfuncs-list-name experimental_v0.1.0 "$file" "compiled_v1/$name.json"
  $SIERRA_PATH --allowed-libfuncs-list-name experimental_v0.1.0 --add-pythonic-hints "compiled_v1/$name.json" "compiled_v1/$name.casm"
done < <(find src_v1 -name "*.cairo" -type f -print0)
popd
echo "Done!"

pushd "$(dirname "$0")" || exit 1
fetch_compilers "2.0.0" "cairo_compilers_v2"
mkdir -p "compiled_v2"

echo "Compiling v2 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH "$file" "compiled_v2/$name.json"
  $SIERRA_PATH --add-pythonic-hints "compiled_v2/$name.json" "compiled_v2/$name.casm"
done < <(find src_v2 -name "*.cairo" -type f -print0)
popd
echo "Done!"
