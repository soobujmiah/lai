#!/usr/bin/env bash
# ADB-first device-testing helper for LAI (docs/TESTING.md "ADB-first testing").
#
# Reuses plain `adb`/`am`/`pm`/`dumpsys` — no new dependency, no UIAutomator, no coordinate
# taps. Every subcommand waits on an observable condition (process state, activity draw
# completion, or a targeted logcat pattern) instead of a fixed sleep, and every log read is
# filtered to the tags this app actually writes (LAI-*), never a raw full-log dump.
#
# Package defaults to the signed release id; override with LAI_PKG for a debug build
# (dev.lai.runtime.debug).
set -euo pipefail

PKG="${LAI_PKG:-dev.lai.runtime}"
ACTIVITY="$PKG/dev.lai.runtime.MainActivity"
LOG_TAGS='LAI-qualify|LAI-model|LAI-llm|LAI-lifecycle'

usage() {
  cat <<'EOF'
Usage: lai_adb.sh <command> [args]

  install <apk-path>              adb install -r the given APK
  reset                            force-stop the app for a clean starting state
  launch                           reset, then launch and block until first frame is drawn
  wait-process [timeout-seconds]   poll until the app process exists (default 30s)
  wait-log <regex> [timeout-seconds]
                                    poll logcat until a line matches (default 60s)
  logs [lines]                     dump the last N lines of LAI-tagged logcat (default 200)
  state                            print process/activity/version summary (dumpsys, targeted)
  qualify <model-id> <backend-id> [prompt] [timeout-seconds]
                                    reset, launch with a qualification intent that forces
                                    <model-id> onto <backend-id> and runs one real generation,
                                    then block on the terminal LAI-qualify log line and print
                                    the evidence. Exit code: 0 DONE/ready, 1 DENIED
                                    (backend not in this build's VALIDATED_ACCELERATORS),
                                    2 LOAD_FAILED, 3 timed out with no terminal state.

Environment:
  LAI_PKG   application id (default dev.lai.runtime; use dev.lai.runtime.debug for debug builds)
EOF
}

require_device() {
  if ! adb get-state >/dev/null 2>&1; then
    echo "No ADB device/emulator connected (adb get-state failed)" >&2
    exit 1
  fi
}

cmd_install() {
  local apk="$1"
  adb install -r "$apk"
}

cmd_reset() {
  adb shell am force-stop "$PKG"
}

# `am start -W` blocks until the activity has actually drawn its first frame — an observable
# readiness condition, not a guess about how long cold start takes.
cmd_launch() {
  cmd_reset
  adb shell am start -W -n "$ACTIVITY"
}

cmd_wait_process() {
  local timeout="${1:-30}"
  local waited=0
  while (( waited < timeout )); do
    if adb shell pidof -s "$PKG" >/dev/null 2>&1; then
      echo "process up after ${waited}s"
      return 0
    fi
    sleep 1
    waited=$(( waited + 1 ))
  done
  echo "process did not appear within ${timeout}s" >&2
  return 1
}

cmd_wait_log() {
  local pattern="$1"
  local timeout="${2:-60}"
  local waited=0
  local match
  while (( waited < timeout )); do
    match=$(adb logcat -d -e "$pattern" 2>/dev/null | tail -1)
    if [[ -n "$match" ]]; then
      echo "$match"
      return 0
    fi
    sleep 1
    waited=$(( waited + 1 ))
  done
  echo "no match for /$pattern/ within ${timeout}s" >&2
  return 1
}

cmd_logs() {
  local lines="${1:-200}"
  adb logcat -d -t "$lines" | grep -E "$LOG_TAGS" || true
}

cmd_state() {
  echo "--- process ---"
  adb shell pidof -s "$PKG" 2>/dev/null && echo "running" || echo "not running"
  echo "--- version ---"
  adb shell dumpsys package "$PKG" | grep -E "versionName|versionCode" | head -2
  echo "--- top activity ---"
  adb shell dumpsys activity activities 2>/dev/null | grep -E "topResumedActivity|mResumedActivity" | head -2
}

cmd_qualify() {
  local model_id="$1" backend_id="$2" prompt="${3:-}" timeout="${4:-180}"
  cmd_reset
  adb logcat -c
  local args=(-n "$ACTIVITY" --es qualify_backend "$backend_id" --es qualify_model "$model_id")
  if [[ -n "$prompt" ]]; then
    args+=(--es qualify_prompt "$prompt")
  fi
  echo "launching qualification: model=$model_id backend=$backend_id"
  adb shell am start -W "${args[@]}"

  local waited=0
  local line=""
  while (( waited < timeout )); do
    line=$(adb logcat -d -e 'LAI-qualify' 2>/dev/null | grep -E 'DONE|DENIED|LOAD_FAILED' | tail -1)
    if [[ -n "$line" ]]; then
      break
    fi
    sleep 2
    waited=$(( waited + 2 ))
  done

  echo "=== qualification evidence (model=$model_id backend=$backend_id) ==="
  adb logcat -d | grep -E "$LOG_TAGS" || true
  echo "==="

  if [[ -z "$line" ]]; then
    echo "TIMED OUT waiting for a terminal LAI-qualify state within ${timeout}s" >&2
    return 3
  fi
  case "$line" in
    *DENIED*) return 1 ;;
    *LOAD_FAILED*) return 2 ;;
    *DONE*) return 0 ;;
  esac
}

main() {
  local command="${1:-}"
  [[ -n "$command" ]] || { usage; exit 1; }
  shift || true
  require_device
  case "$command" in
    install) cmd_install "$@" ;;
    reset) cmd_reset "$@" ;;
    launch) cmd_launch "$@" ;;
    wait-process) cmd_wait_process "$@" ;;
    wait-log) cmd_wait_log "$@" ;;
    logs) cmd_logs "$@" ;;
    state) cmd_state "$@" ;;
    qualify) cmd_qualify "$@" ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $command" >&2; usage; exit 1 ;;
  esac
}

main "$@"
