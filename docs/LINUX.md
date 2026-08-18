# Linux / Terminal

LAI shall provide **managed Linux** where practical — not a root shell.

## Current

**Missing.** HyperOS restricts `MANAGE_EXTERNAL_STORAGE`, no PRoot/QEMU. `platform:shizuku` is `READY_UID_2000` where available.

## Target (long-term, after Workstation)

*   **Runtime:** PRoot/QEMU distro/rootfs (`Debian` as in `ternux`), `mounts` (project → container), `processes` (bounded, cancellable, `EMERGENCY_STOP`), `packages` (`apt` where available), `profiles`, `display` (Xfce4 via `Zink/Turnip` Vulkan where NpuHub’s `ternux` already does — no root).
*   **Capabilities:** `shell` (`argv` allowlist, no raw `sh -c`), `filesystem` (`ls/cp/mv`), `process` (`ps/kill`), `package` (`apt` bounded), `compilers` (`clang/CMake/NDK`), `interpreters` (`python/node`), `build` (`make/gradle`), `scripts` (`sh`), `networking` (`curl` to allowlist `huggingface.co`/`raw.githubusercontent.com` only for models/docs, never user content), `diagnostics` (`top/free/df` bounded), `monitoring`.
*   **Terminal UI:** `Terminal` tab (like `ScreenReader`/`Automator`), `xterm` with `imePadding()` + `imeAnimationTarget` (same as Chat), `Logs` export via SAF, `Diagnostics` privacy-safe.

## Constraints

**Never assume root, never assume `MANAGE_EXTERNAL_STORAGE`, never assume `storage/LAI` is `mmap`-able.** Detect: `Shizuku` `READY` → `ElevatedShell` (argv), `SAF` `GRANTED` → `WorkspaceSaf`, `PRoot` `AVAILABLE` → `linux` runtime, else `UNKNOWN`/`N/A` + graceful fallback (show `install ternux` hint).

## Architecture

`features:linux` (UI) → `runtime:linux` (PRoot/QEMU adapter) → `platform:workspace` (SAF) + `platform:shizuku` (argv). No feature calls `Runtime.exec("su")`.

## Testing

`install/start/stop/kill/reboot` of distro, `storage full` during `apt`, `process kill` mid-compile, `install -r` grant survival, `validate_repo.sh` still `128 MB`.
