#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
assert_code() {
    local actual
    actual="$(bash scripts/version-code.sh "$1")"
    [[ "$actual" == "$2" ]] || { echo "$1: expected $2, got $actual" >&2; exit 1; }
}
assert_code 0.0.0-dev 1
assert_code 0.0.1 1
assert_code 0.1.0 1000
assert_code 1.0.0 1000000
assert_code 1.2.3 1002003
assert_code 1.2.3-rc.1 1002003
assert_code 2099.999.999 2099999999
assert_code 2100.0.0 2100000000
for invalid in 1.2 01.2.3 1.02.3 1.2.03 1.1000.0 1.0.1000 2100.0.1 999999999999.0.0 '../1.2.3' '1.2.3;echo bad'; do
    if bash scripts/version-code.sh "$invalid" >/dev/null 2>&1; then
        echo "Unexpectedly accepted: $invalid" >&2
        exit 1
    fi
done
printf 'Version-code tests passed.\n'
