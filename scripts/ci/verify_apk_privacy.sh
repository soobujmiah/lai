#!/usr/bin/env bash
set -euo pipefail

build_type=${1:?debug or release required}
connected=$(find "app/build/outputs/apk/connected/$build_type" -maxdepth 1 -name '*.apk' -print -quit)
airgap=$(find "app/build/outputs/apk/airgap/$build_type" -maxdepth 1 -name '*.apk' -print -quit)
[[ -n "$connected" && -f "$connected" ]] || { echo "Connected APK not found" >&2; exit 2; }
[[ -n "$airgap" && -f "$airgap" ]] || { echo "Air-gapped APK not found" >&2; exit 2; }

connected_permissions=$(apkanalyzer manifest permissions "$connected")
airgap_permissions=$(apkanalyzer manifest permissions "$airgap")

grep -q 'android.permission.INTERNET' <<<"$connected_permissions" || {
  echo "Connected APK unexpectedly lacks INTERNET permission" >&2
  exit 3
}
if grep -q 'android.permission.INTERNET' <<<"$airgap_permissions"; then
  echo "Air-gapped APK contains INTERNET permission" >&2
  exit 4
fi
if grep -q 'android.permission.ACCESS_NETWORK_STATE' <<<"$airgap_permissions"; then
  echo "Air-gapped APK contains ACCESS_NETWORK_STATE permission" >&2
  exit 5
fi

connected_id=$(apkanalyzer manifest application-id "$connected")
airgap_id=$(apkanalyzer manifest application-id "$airgap")
[[ "$connected_id" == "dev.lai.runtime" || "$connected_id" == "dev.lai.runtime.debug" ]] || {
  echo "Unexpected connected application id: $connected_id" >&2; exit 6;
}
[[ "$airgap_id" == "dev.lai.runtime.airgap" || "$airgap_id" == "dev.lai.runtime.airgap.debug" ]] || {
  echo "Unexpected air-gapped application id: $airgap_id" >&2; exit 7;
}

printf 'APK privacy verified: connected=%s; airgap=%s (no network permissions)\n' "$connected_id" "$airgap_id"
