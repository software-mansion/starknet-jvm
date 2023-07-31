#!/bin/bash

SRC_V0="src_v0"
SRC_V1="src_v1"
SRC_V2="src_v2"

COMPILE_V0="compiled_v0"
COMPILE_V1="compiled_v1"
COMPILE_V2="compiled_v2"

COMPILER_BIN_V1="cairo_compilers_v1"
COMPILER_BIN_V2="cairo_compilers_v2"

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
  else
    echo "Unsupported OS: $OS"
    exit 1
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
  local OUT_DIR=$2
  local COMPILER_URL=$(get_release_url "$VERSION")

  echo "Fetching compiler v$VERSION binaries..."
  echo "URL: $COMPILER_URL"

  mkdir -p "$OUT_DIR"
  curl -LsSf "$COMPILER_URL" | tar -xz -C "$OUT_DIR" || exit 1

  COMPILER_PATH=$(pwd)/"$OUT_DIR"/cairo/bin/starknet-compile
  SIERRA_PATH=$(pwd)/"$OUT_DIR"/cairo/bin/starknet-sierra-compile

  echo "Done!"
}

pushd "$(dirname "$0")" || exit
mkdir -p "$COMPILE_V0"

echo "Compiling v0 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  if [[ $name == *"account"* ]]; then
    starknet-compile-deprecated "$file" --account_contract --output "$COMPILE_V0/$name.json" --abi "$COMPILE_V0/${name}Abi.json" || exit 1
  else
    starknet-compile-deprecated "$file" --output "$COMPILE_V0/$name.json" --abi "$COMPILE_V0/${name}Abi.json" || exit 1
  fi
done < <(find "$SRC_V0" -name "*.cairo" -type f -print0)
popd
echo "Done!"

pushd "$(dirname "$0")" || exit 1
fetch_compilers "1.1.1" "$COMPILER_BIN_V1"
mkdir -p "$COMPILE_V1"

echo "Compiling v1 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH --allowed-libfuncs-list-name experimental_v0.1.0 "$file" "$COMPILE_V1/$name.json" || exit 1
  $SIERRA_PATH --allowed-libfuncs-list-name experimental_v0.1.0 --add-pythonic-hints "$COMPILE_V1/$name.json" "$COMPILE_V1/$name.casm" || exit 1
done < <(find "$SRC_V1" -name "*.cairo" -type f -print0)
popd
echo "Done!"

pushd "$(dirname "$0")" || exit 1
fetch_compilers "2.0.0" "$COMPILER_BIN_V2"
mkdir -p "$COMPILE_V2"

echo "Compiling v2 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH "$file" "$COMPILE_V2/$name.json" || exit 1
  $SIERRA_PATH --add-pythonic-hints "$COMPILE_V2/$name.json" "$COMPILE_V2/$name.casm" || exit 1
done < <(find "$SRC_V2" -name "*.cairo" -type f -print0)
popd
echo "Done!"
