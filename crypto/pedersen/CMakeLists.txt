cmake_minimum_required (VERSION 3.5)
if (NOT DEFINED CMAKE_CXX_COMPILER)
  set(CMAKE_CXX_COMPILER /usr/bin/clang++)
endif()

project(StarkwareCryptoLib VERSION 0.1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)

set(BOOST_ENABLE_CMAKE ON)

include(FetchContent)
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/609281088cfefc76f9d0ce82e1ff6c30cc3591e5.zip
)
# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)


# Checking if current build supports __uint128_t and including boost if it doesn't.
INCLUDE(CheckCXXSourceCompiles)
check_cxx_source_compiles("
int main() {
  __uint128_t a = 0;
  return a;
}" HAVE_UINT128)

if (NOT HAVE_UINT128)
  FetchContent_Declare(
    boostorg
    URL https://archives.boost.io/release/1.79.0/source/boost_1_79_0.tar.gz
  )

  FetchContent_GetProperties(boostorg)
  if (NOT boostorg_POPULATED)
      FetchContent_Populate(boostorg)
  endif ()

  include_directories(${boostorg_SOURCE_DIR})

  # For Windows: Prevent overriding the parent project's compiler/linker settings
  set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
  FetchContent_MakeAvailable(boostorg)
endif()

if (NOT DEFINED JAVA_INCLUDE_PATH)
  set(JAVA_AWT_LIBRARY NotNeeded)
  set(JAVA_JVM_LIBRARY NotNeeded)
  set(JAVA_AWT_INCLUDE_PATH NotNeeded)
  set(JAVA_INCLUDE_PATH "$ENV{JAVA_HOME}/include")
  set(JAVA_INCLUDE_PATH2 "$ENV{JAVA_HOME}/include/darwin;$ENV{JAVA_HOME}/include/linux")

  find_package(Java REQUIRED)
  find_package(JNI REQUIRED)
  include_directories(${JNI_INCLUDE_DIRS})
endif()

include(CTest)
enable_testing()
include_directories("${CMAKE_SOURCE_DIR}/crypto-cpp/src")
include_directories("${CMAKE_SOURCE_DIR}/bindings")

if (NOT DEFINED CMAKE_BUILD_TYPE)
  set(CMAKE_BUILD_TYPE Debug)
endif()

set(CMAKE_EXPORT_COMPILE_COMMANDS ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -Werror -Wall -Wextra -fno-strict-aliasing -fPIC")

set(CC_OPTIMIZE "-O3")

set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} ${CC_OPTIMIZE}")

add_subdirectory(crypto-cpp/src)
add_subdirectory(bindings)

