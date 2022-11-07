#!/bin/bash

if [ -z "$TNN_ROOT" ]; then
   echo -e "Please build TNN and set TNN_ROOT environment variable"
   exit -1
fi

SCRIPT=$(realpath "$0")
DIR=$(dirname "$SCRIPT")

mkdir -p $DIR/build
cd $DIR/build

cmake ..
make -j8

