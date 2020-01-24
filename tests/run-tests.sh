#!/usr/bin/env bash

set -euvo pipefail

# This script assumes that it is running from the root of the Wit workspace.

api_firrtl_sifive_path=./api-firrtl-sifive

wake --init .

# This is gross because we don't have a way of preventing Wake from
# automatically picking up .wake files that only make sense in specific contexts
# such as testing.
ln -snf "test.wake.template" "$api_firrtl_sifive_path/tests/test.wake"

wake runAPIFIRRTLSiFiveTests Unit
