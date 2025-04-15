cmake_minimum_required (VERSION 3.5)

project(poseidon)

include_directories("${CMAKE_SOURCE_DIR}/poseidon/sources")
include_directories("${CMAKE_SOURCE_DIR}/bindings")

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

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Werror -O3 -fPIC")
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

add_subdirectory(poseidon)
add_subdirectory(bindings)
