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
LOG_TAGS='LAI-qualify|LAI-model|LAI-llm|LAI-lifecycle|LAI-diag'

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
                                    the evidence. The app itself gives up waiting on a stuck
                                    native load after ~45s (LOAD_TIMEOUT) by default, so this
                                    script's own [timeout-seconds] is now just an outer safety
                                    margin, not the only bound. Exit code: 0 DONE/ready,
                                    1 DENIED (backend not in this build's
                                    VALIDATED_ACCELERATORS), 2 LOAD_FAILED, 4 LOAD_TIMEOUT (the
                                    app detected and gave up on a stuck native call itself),
                                    5 MODEL_NOT_FOUND (the model never appeared in
                                    installedModels -- a startup race, not a load problem),
                                    3 this script's own timeout with no terminal state at all
                                    (worse than 4/5: the app never even reported one itself).
  probe <backend-id> [timeout-seconds]
                                    reset, launch with a model-free capabilities probe (no
                                    load, no generation) and block on the terminal LAI-diag
                                    "probe: DONE" line. Seconds, not minutes, when backend
                                    enumeration is healthy — run this before `qualify` to
                                    isolate an enumeration hang from a load/session hang.
                                    Exit code: 0 DONE seen, 3 timed out.

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

# Repeated `adb logcat -d -e '<tag>'` polling has a real, observed failure mode: each
# invocation is itself echoed into the device's own logcat by adbd ("in ShellService: ...
# exec logcat ... '<tag>'"), which -- because that echoed line literally contains the tag
# text -- matches the very filter it's polluting. At a 1-2s polling interval this
# self-referential noise, plus ordinary system log volume, can rotate a real target line out
# of the buffer before a later poll ever reads it (confirmed: a `probe: DONE` line present at
# T+0 was gone from `-e` output well within 30s of 1s-interval polling on this device). Filter
# by exact PID instead: it can't match its own invocation (a PID number isn't the search text
# a text-matching filter would echo) and it excludes unrelated system/app noise entirely,
# leaving far more buffer headroom for the signal actually being waited on.
pid_of_app() {
  adb shell pidof -s "$PKG" 2>/dev/null | tr -d '\r'
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
  local pid
  pid=$(pid_of_app)
  while (( waited < timeout )); do
    if [[ -n "$pid" ]]; then
      match=$( (adb logcat -d --pid="$pid" 2>/dev/null || true) | (grep -E "$pattern" || true) | tail -1)
    fi
    if [[ -n "$match" ]]; then
      echo "$match"
      return 0
    fi
    sleep 1
    waited=$(( waited + 1 ))
    [[ -z "$pid" ]] && pid=$(pid_of_app)
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


# `adb shell` re-joins its argv into one string and the *remote* shell re-tokenizes it, so a
# value containing spaces (e.g. a prompt) silently loses its quoting and gets split into
# separate `am start` arguments (observed: "Say hello" became two stray positional args, one
# of which `am` misread as a package name). Route through a single pre-quoted remote command
# string instead of an argv array so values survive intact.
shell_quote() {
  printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"
}

cmd_qualify() {
  local model_id="$1" backend_id="$2" prompt="${3:-}" timeout="${4:-180}"
  cmd_reset
  adb logcat -c
  local remote_cmd="am start -W -n $ACTIVITY --es qualify_backend $(shell_quote "$backend_id") --es qualify_model $(shell_quote "$model_id")"
  if [[ -n "$prompt" ]]; then
    remote_cmd+=" --es qualify_prompt $(shell_quote "$prompt")"
  fi
  echo "launching qualification: model=$model_id backend=$backend_id"
  adb shell "$remote_cmd"
  local pid
  pid=$(pid_of_app)

  local waited=0
  local line=""
  while (( waited < timeout )); do
    if [[ -n "$pid" ]]; then
      line=$( (adb logcat -d --pid="$pid" 2>/dev/null || true) | (grep -E 'DONE|DENIED|LOAD_FAILED|LOAD_TIMEOUT|MODEL_NOT_FOUND' || true) | tail -1)
    fi
    if [[ -n "$line" ]]; then
      break
    fi
    sleep 2
    waited=$(( waited + 2 ))
    [[ -z "$pid" ]] && pid=$(pid_of_app)
  done

  echo "=== qualification evidence (model=$model_id backend=$backend_id) ==="
  if [[ -n "$pid" ]]; then
    adb logcat -d --pid="$pid" 2>/dev/null | grep -E "$LOG_TAGS" || true
  fi
  echo "==="

  if [[ -z "$line" ]]; then
    echo "TIMED OUT (script-level) waiting for a terminal LAI-qualify state within ${timeout}s -- the app itself never reported LOAD_TIMEOUT either, which is a worse sign than exit 4" >&2
    return 3
  fi
  case "$line" in
    *DENIED*) return 1 ;;
    *LOAD_FAILED*) return 2 ;;
    *LOAD_TIMEOUT*) return 4 ;;
    *MODEL_NOT_FOUND*) return 5 ;;
    *DONE*) return 0 ;;
  esac
}

cmd_probe() {
  local backend_id="$1" timeout="${2:-30}"
  cmd_reset
  adb logcat -c
  local remote_cmd="am start -W -n $ACTIVITY --es qualify_backend $(shell_quote "$backend_id") --ez qualify_probe true"
  echo "launching probe: backend=$backend_id"
  adb shell "$remote_cmd"
  local pid
  pid=$(pid_of_app)

  local waited=0
  local line=""
  while (( waited < timeout )); do
    if [[ -n "$pid" ]]; then
      line=$( (adb logcat -d --pid="$pid" 2>/dev/null || true) | (grep -E 'probe: DONE' || true) | tail -1)
    fi
    if [[ -n "$line" ]]; then
      break
    fi
    sleep 1
    waited=$(( waited + 1 ))
    [[ -z "$pid" ]] && pid=$(pid_of_app)
  done

  echo "=== probe evidence (backend=$backend_id) ==="
  if [[ -n "$pid" ]]; then
    adb logcat -d --pid="$pid" 2>/dev/null | grep -E "$LOG_TAGS" || true
  fi
  echo "==="

  if [[ -z "$line" ]]; then
    echo "TIMED OUT waiting for 'probe: DONE' within ${timeout}s -- enumeration itself may be hanging" >&2
    return 3
  fi
  return 0
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
    probe) cmd_probe "$@" ;;
    -h|--help|help) usage ;;
    *) echo "Unknown command: $command" >&2; usage; exit 1 ;;
  esac
}

main "$@"
