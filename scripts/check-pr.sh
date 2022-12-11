#!/usr/bin/env bash

if [ $# -lt 2 ]; then
  echo "usage: $0 <pull-request-number> <branch-name>"
  exit 1
fi

git fetch origin pull/$1/head:$2
