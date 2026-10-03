#!/usr/bin/env bash
set -euo pipefail

# Apache-2.0 traineddata from tesseract-ocr/tessdata_fast. The model binaries are
# staged on the GitHub runner and packaged as app assets; they are not committed.
source_commit=87416418657359cb625c412a48b6e1d6d41c29bd
destination=runtime/ocr/src/main/assets/tessdata
mkdir -p "$destination"

stage_model() {
  local language=$1 expected=$2 file="$destination/$1.traineddata"
  curl --fail --location --silent --show-error --retry 3 \
    "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/$source_commit/$language.traineddata" \
    --output "$file"
  printf '%s  %s\n' "$expected" "$file" | sha256sum --check --status
}

stage_model ben 31163084c279aaebd376216f0c3d5c17ad4b5fee8db49dae79c20000b5de5964
stage_model eng 7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2
echo "Pinned offline OCR models staged and SHA-256 verified"
