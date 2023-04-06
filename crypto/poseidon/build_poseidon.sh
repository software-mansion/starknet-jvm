#!/bin/bash

set -e

if [[ -z "${JAVA_HOME}" ]]; then
  echo "JAVA_HOME not set!"
  exit 1
fi

rm *libposeidon_jni.* || true
rm *lib_pos.* || true

build_for_mac_target () {
  MACARCH=$(awk -F '-' '{print $1}' <<< $1)
  # First build poseidon
  pushd poseidon/sources
  sed "/CFLAGS = -Wall -O3 -fPIC/ s/$/ -target $1/" Makefile > "Makefile_$MACARCH"
  make -f "Makefile_$MACARCH" only_c
  rm *.o "Makefile_$MACARCH"
  popd

  # Then build bindings
  pushd bindings
  sed "/CFLAGS = -Wall -Werror -O3 -fPIC/ s/$/ -target $1/" Makefile > "Makefile_$MACARCH"
  LDFLAGS="-I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin" \
    make -f "Makefile_$MACARCH" libposeidon_jni.so
  rm "Makefile_$MACARCH"
  popd

  mv poseidon/sources/lib_pos.so "$(awk -F '-' '{print $1}' <<< $1)_lib_pos.dylib"
  mv bindings/libposeidon_jni.so "$(awk -F '-' '{print $1}' <<< $1)_libposeidon_jni.dylib"
}

echo "Building poseidon..."
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
  make -C poseidon/sources only_c
  LDFLAGS="-I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux" \
    make -C bindings libposeidon_jni.so
  mv bindings/libposeidon_jni.so libposeidon_jni.so
  mv poseidon/sources/lib_pos.so lib_pos.so
elif [[ "$OSTYPE" == "darwin"* ]]; then
  build_for_mac_target "x86_64-apple-macos11"
  build_for_mac_target "arm64-apple-macos11"
  lipo -create -output lib_pos.dylib x86_64_lib_pos.dylib arm64_lib_pos.dylib
  lipo -create -output libposeidon_jni.dylib x86_64_libposeidon_jni.dylib arm64_libposeidon_jni.dylib
  find . -type f -not -name libposeidon_jni.dylib -not -name lib_pos.dylib -name '*.dylib' -delete
else
  echo "OS not supported at the moment"
  exit 1;
fi

make -C poseidon/sources clean

echo "Poseidon built!"
