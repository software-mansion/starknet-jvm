#! /bin/bash

echo "Building poseidon..."
cmake -Bbuild -DCMAKE_OSX_ARCHITECTURES="x86_64;arm64"
make -C build
echo "Poseidon built!"
