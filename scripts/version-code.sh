#!/usr/bin/env bash
set -euo pipefail
version="${1:?Usage: version-code.sh VERSION}"
if [[ ! "$version" =~ ^(0|[1-9][0-9]{0,3})\.(0|[1-9][0-9]{0,2})\.(0|[1-9][0-9]{0,2})(-[0-9A-Za-z]+([.-][0-9A-Za-z]+)*)?$ ]]; then
    echo "Expected MAJOR.MINOR.PATCH, with an optional prerelease suffix. Minor and patch must be below 1000." >&2
    exit 1
fi
code=$((BASH_REMATCH[1] * 1000000 + BASH_REMATCH[2] * 1000 + BASH_REMATCH[3]))
if ((code > 2100000000)); then
    echo "Version exceeds Android's versionCode limit." >&2
    exit 1
fi
if ((code == 0)); then code=1; fi
printf '%s\n' "$code"
