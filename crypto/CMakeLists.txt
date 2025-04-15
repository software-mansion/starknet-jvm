cmake_minimum_required(VERSION 3.5)

project(CryptoLibs)

include_directories("${CMAKE_SOURCE_DIR}/poseidon/poseidon/sources")
include_directories("${CMAKE_SOURCE_DIR}/poseidon/bindings")

include_directories("${CMAKE_SOURCE_DIR}/pedersen/crypto-cpp/src")
include_directories("${CMAKE_SOURCE_DIR}/pedersen/bindings")

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--zmax-page-size=0x4000")

set(JAVA_AWT_LIBRARY NotNeeded)
set(JAVA_JVM_LIBRARY NotNeeded)
set(JAVA_AWT_INCLUDE_PATH NotNeeded)
set(JAVA_INCLUDE_PATH "$ENV{JAVA_HOME}/include")
set(JAVA_INCLUDE_PATH2 "$ENV{JAVA_HOME}/include/darwin;$ENV{JAVA_HOME}/include/linux")

find_package(Java REQUIRED)
find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})

add_subdirectory(poseidon)
add_subdirectory(pedersen)
