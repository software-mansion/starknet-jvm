#!/bin/bash
set -e

mkdir -p build/libs/shared
(cd ../crypto && ./build.sh) || exit

# Move libraries to a proper directory
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  ARCH=$(arch)
  SUFFIX="linux/$ARCH"
elif [[ "$OSTYPE" == "darwin"* ]]; then
  # Darwin uses a universal binary, no arch is needed
  SUFFIX="darwin"
else
  echo "OS not supported at the moment"
  exit 1;
fi

mkdir -p build/libs/shared/$SUFFIX
cp -f ../crypto/build/bindings/libcrypto_jni.* build/libs/shared/$SUFFIX
