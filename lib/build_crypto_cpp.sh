#!/bin/bash

mkdir -p build/libs/shared
(cd ../crypto && ./build.sh) || exit
cp -f ../crypto/build/bindings/libcrypto_jni.* build/libs/shared
