#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

limit=$((128 * 1024 * 1024))
size=$(find . -path './.git' -prune -o -path '*/build' -prune -o -type f -printf '%s\n' | awk '{s+=$1} END {print s+0}')
if (( size > limit )); then
  echo "Repository source footprint ${size} exceeds 128 MB" >&2
  exit 1
fi

forbidden=$(find . -path './.git' -prune -o -type f \( \
  -name '*.apk' -o -name '*.aab' -o -name '*.aar' -o -name '*.so' -o \
  -name '*.gguf' -o -name '*.onnx' -o -name '*.tflite' -o -name '*.dlc' -o \
  -name '*.jks' -o -name '*.keystore' -o -name 'gradle-wrapper.jar' \
\) -print)
if [[ -n "$forbidden" ]]; then
  echo "Generated/heavy files are forbidden:" >&2
  echo "$forbidden" >&2
  exit 1
fi

required=(
  README.md
  docs/ARCHITECTURE.md
  docs/STATUS.md
  docs/SECURITY_AND_SAFETY.md
  .github/workflows/android_build.yml
)
for path in "${required[@]}"; do
  [[ -s "$path" ]] || { echo "Missing required documentation/config: $path" >&2; exit 1; }
done

if grep -RInE '(ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})' \
  --exclude-dir=.git --exclude='validate_repo.sh' .; then
  echo "Possible GitHub token found in repository" >&2
  exit 1
fi

python3 scripts/check_architecture_boundaries.py
python3 scripts/validate_model_catalog.py
python3 scripts/validate_documentation.py

printf 'Source policy OK: %d bytes (limit %d)\n' "$size" "$limit"
