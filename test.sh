#!/usr/bin/env bash
# Simple sanity check
# Requirements
#  - wit >= 0.6
#  - wake >= 0.15
#  - protoc

CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo "Running test in $CWD..."
cd $CWD

# Create workspace
wit init test-ws -a $CWD
cd test-ws
wake --init .

# Ensure that firrtl compiles and can compile a firrtl file
export WAKE_PATH=$(dirname `which protoc`)
wake 'source "firrtl/regress/ICache.fir" | makeFirrtlCompilePlan firrtlScalaModule.scalaModuleClasspath "test" "build" | runFirrtlCompile | getFirrtlCompileOutputsTargetOutputs'
