#! /bin/bash

echo "Building crypto-cpp..."
cmake -DCMAKE_BUILD_TYPE=Release -Bbuild -DCMAKE_OSX_ARCHITECTURES="x86_64;arm64"
make -C build
echo "Crypto-cpp built!"
