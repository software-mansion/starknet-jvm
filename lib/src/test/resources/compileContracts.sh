#!/bin/bash

readonly OS=$(uname)
readonly ARCH=$(uname -m)

readonly REPO_ROOT=$(git rev-parse --show-toplevel)

readonly V0_CONTRACT_PATH="contracts_v0"
readonly V1_CONTRACT_PATH="contracts_v1"
readonly V2_CONTRACT_PATH="contracts_v2"

readonly V0_ARTIFACT_PATH="contracts_v0/target/release"

readonly V2_COMPILER_BIN_PATH="compilers/v2"
readonly V2_COMPILER_VERSION="2.2.0"

readonly V1_SCARB_VERSION="0.4.0"
readonly V2_SCARB_VERSION="0.7.0"

if [ "$OS" == "Linux" ] && [ "$ARCH" == "x86_64" ]; then
  true
elif [ "$OS" == "Darwin" ] && [ "$ARCH" == "arm64" ]; then
  true
elif [ ! -d "$V2_COMPILER_BUILD_PATH" ]; then
  echo "Your OS or architecture ($ARCH-$OS) is not supported directly."
  echo "To proceed, please set V2_COMPILER_BUILD_PATH environment variable to a directory with Cairo v$V2_COMPILER_VERSION compiler binaries built for ($ARCH-$OS)."
  exit 1
fi

fetch_compilers() {
  local VERSION=$1
  local OUT_DIR=$2

  local VERSION_SHORT=${VERSION:0:1}
  if [ "$VERSION_SHORT" == "1" ]; then
    local COMPILER_BUILD_PATH="$V1_COMPILER_BUILD_PATH"
  elif [ "$VERSION_SHORT" == "2" ]; then
    local COMPILER_BUILD_PATH="$V2_COMPILER_BUILD_PATH"
  else
    echo "Invalid compiler version: $VERSION"
    exit 1
  fi

  echo "Fetching compiler v$VERSION binaries..."

  mkdir -p "$OUT_DIR"

  if [ "$OS" == "Linux" ] && [ "$ARCH" == "x86_64" ]; then
    echo "Source: https://github.com/starkware-libs/cairo/releases/"
    curl -LsSf "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-x86_64-unknown-linux-musl.tar.gz" | tar -xz -C "$OUT_DIR" || exit 1
  elif [ "$OS" == "Darwin" ] && [ "$ARCH" == "arm64" ]; then
    echo "Source: https://github.com/starkware-libs/cairo/releases/"
    curl -LsSf "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-aarch64-apple-darwin.tar" | tar -x -C "$OUT_DIR" || exit 1
  else
    if [ -n "$COMPILER_BUILD_PATH" ] && [ -d "$COMPILER_BUILD_PATH" ]; then
      echo "Source: $COMPILER_BUILD_PATH..."

      rm -r "$(dirname "$0")/$OUT_DIR/cairo/bin/" || true
      rm -r "$(dirname "$0")/$OUT_DIR/cairo/corelib/" || true

      mkdir -p "$(dirname "$0")/$OUT_DIR/cairo/bin/"
      mkdir -p "$(dirname "$0")/$OUT_DIR/cairo/corelib/"

      echo "Copying binaries..."
      rsync -aW "${COMPILER_BUILD_PATH}/" "$(dirname "$0")/$OUT_DIR/cairo/bin/" || exit 1
      echo "Copying corelib..."
      rsync -aW "$REPO_ROOT/cairo$VERSION_SHORT/corelib/" "$(dirname "$0")/$OUT_DIR/cairo/corelib/" || exit 1
    else
      echo "Compiler binaries not found for $OS-$ARCH."
      exit 1
    fi
  fi

  COMPILER_PATH=$(pwd)/"$OUT_DIR"/cairo/bin/starknet-compile
  SIERRA_PATH=$(pwd)/"$OUT_DIR"/cairo/bin/starknet-sierra-compile

  echo "Done!"
}

#TODO (#326): remove once legacy devnet client is removed
echo "Fetching Cairo compilers to support legacy devnet..."
pushd "$(dirname "$0")" || exit 1
fetch_compilers "$V2_COMPILER_VERSION" "$V2_COMPILER_BIN_PATH"
popd

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
popd
echo "Done!"


echo "Compiling v1 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V1_CONTRACT_PATH" || exit 1
asdf install scarb $V1_SCARB_VERSION || true
asdf local scarb $V1_SCARB_VERSION || exit 1
scarb --profile release build
popd
popd
echo "Done!"

echo "Compiling v2 contracts.."
pushd "$(dirname "$0")" || exit 1
pushd "$V2_CONTRACT_PATH" || exit 1
asdf install scarb $V2_SCARB_VERSION || true
asdf local scarb $V2_SCARB_VERSION || exit 1
scarb --profile release build
popd
popd
echo "Done!"
