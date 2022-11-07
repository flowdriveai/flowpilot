#!/bin/bash

if [ -z "$TNN_ROOT" ]; then
   echo -e "Please build TNN and set TNN_ROOT environment variable"
   exit -1
fi

SCRIPT=$(realpath "$0")
DIR=$(dirname "$SCRIPT")

ABIA64="arm64-v8a"
STL="c++_static"
#STL="gnustl_static"
SHARED_LIB="ON"
ARM="ON"
ANDROID_API_LEVEL="android-19"
BUILD_ANDROID="ON"
# check ANDROID_NDK whether set.
if [ ! -f "$ANDROID_NDK/build/cmake/android.toolchain.cmake" ]; then
   echo -e "Not found: build/cmake/android.toolchain.cmake in ANDROID_NDK:$ANDROID_NDK"
   echo -e "Please download android ndk and set ANDROID_NDK environment variable."
   exit -1
fi

mkdir -p $DIR/build_android
cd $DIR/build_android

cmake .. \
      -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI="${ABIA64}" \
      -DANDROID_STL=${STL} \
      -DANDROID_NATIVE_API_LEVEL=${ANDROID_API_LEVEL}  \
      -DANDROID_TOOLCHAIN=clang \
      -DBUILD_ANDROID=${BUILD_ANDROID}
make -j8

