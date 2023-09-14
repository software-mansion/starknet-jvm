#!/bin/bash

V1_COMPILER_BUILD_PATH_VAR="V1_COMPILER_BUILD_PATH"
V2_COMPILER_BUILD_PATH_VAR="V2_COMPILER_BUILD_PATH"

REPO_ROOT=$(git rev-parse --show-toplevel)

V0_CONTRACT_PATH="src_v0"
V1_CONTRACT_PATH="src_v1"
V2_CONTRACT_PATH="src_v2"

V0_ARTIFACT_PATH="compiled_v0"
V1_ARTIFACT_PATH="compiled_v1"
V2_ARTIFACT_PATH="compiled_v2"

V1_COMPILER_VERSION="1.1.1"
V2_COMPILER_VERSION="2.2.0"

V1_COMPILER_BIN_PATH="compilers/v1"
V2_COMPILER_BIN_PATH="compilers/v2"

fetch_compilers() {
  local VERSION=$1
  local OUT_DIR=$2

  local VERSION_SHORT=${VERSION:0:1}
  [ "$VERSION_SHORT" == "1" ] && local COMPILER_BUILD_PATH_VAR="$V1_COMPILER_BUILD_PATH_VAR"
  [ "$VERSION_SHORT" == "2" ] && local COMPILER_BUILD_PATH_VAR="$V2_COMPILER_BUILD_PATH_VAR"
  local COMPILER_BUILD_PATH="${!COMPILER_BUILD_PATH_VAR}"

  echo "Fetching compiler v$VERSION binaries..."

  mkdir -p "$OUT_DIR"

  OS=$(uname)
  ARCH=$(uname -m)

  if [ "$OS" == "Linux" && "$ARCH" == "x86_64" ]; then
    echo "Source: https://github.com/starkware-libs/cairo/releases/"
    curl -LsSf "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-x86_64-unknown-linux-musl.tar.gz" | tar -xz -C "$OUT_DIR" || exit 1
  elif [ "$OS" == "Darwin" -a "$ARCH" == "arm64" ]; then
    echo "Source: https://github.com/starkware-libs/cairo/releases/"
    curl -LsSf "https://github.com/starkware-libs/cairo/releases/download/v${VERSION}/release-aarch64-apple-darwin.tar" | tar -x -C "$OUT_DIR" || exit 1
  else
      if [ -n "${COMPILER_BUILD_PATH}" ] && [ -d "${COMPILER_BUILD_PATH}" ]; then
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
        echo "Your OS or architecture ($ARCH-$OS) is not supported directly."
        echo "To proceed, please specify a valid path to a built v$VERSION compiler for ($ARCH-$OS) in the $COMPILER_BUILD_PATH_VAR environment variable."
        exit 1
      fi
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
fetch_compilers "$V1_COMPILER_VERSION" "$V1_COMPILER_BIN_PATH"
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
fetch_compilers "$V2_COMPILER_VERSION" "$V2_COMPILER_BIN_PATH"
mkdir -p "$V2_ARTIFACT_PATH"

echo "Compiling v2 contracts.."

while IFS= read -r -d '' file; do
  name="$(basename -- "$file" .cairo)"
  $COMPILER_PATH --single-file "$file" "$V2_ARTIFACT_PATH/$name.json" || exit 1
  $SIERRA_PATH --add-pythonic-hints "$V2_ARTIFACT_PATH/$name.json" "$V2_ARTIFACT_PATH/$name.casm" || exit 1
done < <(find "$V2_CONTRACT_PATH" -name "*.cairo" -type f -print0)
popd
echo "Done!"
