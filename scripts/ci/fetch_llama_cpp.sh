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

# LAI local patch(es) for the pinned llama.cpp, applied on top of the immutable commit.
# See the patch files for what/why. `patch` is available on the GitHub ubuntu runners.
patch_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
for patch_file in "$patch_dir"/ggml-vulkan-skip-mmvq.patch; do
  [[ -f "$patch_file" ]] || continue
  echo "Applying LAI patch: $(basename "$patch_file")"
  (cd "$destination" && patch -p1 --forward < "$patch_file")
done

printf 'Pinned llama.cpp checkout ready: %s\n' "$actual"
