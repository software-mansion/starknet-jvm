#!/bin/bash

REPO_ROOT=$(dirname "$0")/../../../..

V0_CONTRACT_PATH="src_v0"
V1_CONTRACT_PATH="src_v1"
V2_CONTRACT_PATH="src_v2"

V0_ARTIFACT_PATH="compiled_v0"
V1_ARTIFACT_PATH="compiled_v1"
V2_ARTIFACT_PATH="compiled_v2"

COMPILER_BIN_V1="compilers/v1"
COMPILER_BIN_V2="compilers/v2"

build_cairo_compilers() {
  local VERSION=$1
  local OUT_DIR=$2

  local VERSION_SHORT=${VERSION:0:1}

  if ! find "$REPO_ROOT/cairo$VERSION_SHORT/target/debug" -name starknet-compile -type f > /dev/null || ! find "$REPO_ROOT/cairo$VERSION_SHORT/target/debug" -name starknet-sierra-compile -type f > /dev/null; then
    if ! which cargo >/dev/null; then
      echo "Installing rust..."
      curl -sSf https://sh.rustup.rs | sh -s -- -y
    fi
    echo "Building starknet compiler"
    pushd "$REPO_ROOT/cairo$VERSION_SHORT" || exit 1
    cargo build
    cargo run --bin starknet-compile -- --version
    cargo run --bin starknet-sierra-compile -- --version
    popd
  else
    echo "Found existing binaries, skipping compilation."
  fi

  rm -r "$(dirname "$0")/$OUT_DIR/cairo/bin/" || true
  rm -r "$(dirname "$0")/$OUT_DIR/cairo/corelib/" || true

  mkdir -p "$(dirname "$0")/$OUT_DIR/cairo/bin/"
  mkdir -p "$(dirname "$0")/$OUT_DIR/cairo/corelib/"

  echo "Moving binaries..."
  rsync -a "$REPO_ROOT/cairo$VERSION_SHORT/target/debug/" "$(dirname "$0")/$OUT_DIR/cairo/bin/"
  echo "Moving corelib..."
  rsync -a "$REPO_ROOT/cairo$VERSION_SHORT/corelib/" "$(dirname "$0")/$OUT_DIR/cairo/corelib/"
}

fetch_compilers() {
  local VERSION=$1
  local OUT_DIR=$2

  local VERSION_SHORT=${VERSION:0:1}

  echo "Fetching compiler v$VERSION binaries..."

  mkdir -p "$OUT_DIR"

  OS=$(uname)
  ARCH=$(uname -m)

  if [ "$OS" == "Linux" -a "$ARCH" == "x86_64" ]; then
    curl -LsSf "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-x86_64-unknown-linux-musl.tar.gz" | tar -xz -C "$OUT_DIR" || exit 1
  elif [ "$OS" == "Darwin" -a "$ARCH" == "arm64" ]; then
    curl -LsSf "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-aarch64-apple-darwin.tar" | tar -x -C "$OUT_DIR" || exit 1
  elif [ "$OS" == "Darwin" -a "$ARCH" == "x86_64" ]; then
    build_cairo_compilers "$VERSION" "$OUT_DIR"
  else
    echo "Unsupported OS or architecture: $ARCH-$OS"
    exit 1
  fi

  COMPILER_PATH=$(pwd)/"$OUT_DIR"/cairo/bin/starknet-compile
  SIERRA_PATH=$(pwd)/"$OUT_DIR"/cairo/bin/starknet-sierra-compile

  echo "Done!"
}

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
popd
echo "Done!"

pushd "$(dirname "$0")" || exit 1
fetch_compilers "1.1.1" "$COMPILER_BIN_V1"
mkdir -p "$V1_ARTIFACT_PATH"

echo "Compiling v1 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH --allowed-libfuncs-list-name experimental_v0.1.0 "$file" "$V1_ARTIFACT_PATH/$name.json" || exit 1
  $SIERRA_PATH --allowed-libfuncs-list-name experimental_v0.1.0 --add-pythonic-hints "$V1_ARTIFACT_PATH/$name.json" "$V1_ARTIFACT_PATH/$name.casm" || exit 1
done < <(find "$V1_CONTRACT_PATH" -name "*.cairo" -type f -print0)
popd
echo "Done!"

pushd "$(dirname "$0")" || exit 1
fetch_compilers "2.0.2" "$COMPILER_BIN_V2"
mkdir -p "$V2_ARTIFACT_PATH"

echo "Compiling v2 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH "$file" "$V2_ARTIFACT_PATH/$name.json" || exit 1
  $SIERRA_PATH --add-pythonic-hints "$V2_ARTIFACT_PATH/$name.json" "$V2_ARTIFACT_PATH/$name.casm" || exit 1
done < <(find "$V2_CONTRACT_PATH" -name "*.cairo" -type f -print0)
popd
echo "Done!"
