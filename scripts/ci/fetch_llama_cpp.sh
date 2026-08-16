#!/usr/bin/env bash
set -euo pipefail

commit=${1:?immutable llama.cpp commit is required}
destination=${2:?destination directory is required}

case "$commit" in
  *[!0-9a-f]*|'') echo "Invalid llama.cpp commit SHA" >&2; exit 2 ;;
esac
[[ ${#commit} -eq 40 ]] || { echo "llama.cpp commit must be a full 40-character SHA" >&2; exit 2; }

rm -rf "$destination"
git init -q "$destination"
git -C "$destination" remote add origin https://github.com/ggml-org/llama.cpp.git
git -C "$destination" fetch --depth 1 --filter=blob:none origin "$commit"
git -C "$destination" -c advice.detachedHead=false checkout -q FETCH_HEAD
actual=$(git -C "$destination" rev-parse HEAD)
[[ "$actual" == "$commit" ]] || { echo "llama.cpp checkout mismatch: $actual" >&2; exit 3; }
printf 'Pinned llama.cpp checkout ready: %s\n' "$actual"
