# Android Automation

LAI shall provide **permission-aware, auditable Android automation** — never raw shell.

## Current — Device-validated on SM8735

*   **Accessibility:** `AccessibilityAutomationService` (`BIND_ACCESSIBILITY_SERVICE`, `canRetrieveWindowContent`, `canTakeScreenshot` Android 11+ `ScreenshotResult` `hardwareBuffer` → `ARGB_8888`, `flagDefault|flagReportViewIds|flagRetrieveInteractiveWindows|flagIncludeNotImportantViews`, `notificationTimeout 100`, `canPerformGestures true`), `AccessibilityGateway` (`WeakReference`, `connected` `StateFlow`, `updateForegroundPackage`), `NodeSnapshotter` (`≤400` nodes, depth `24`, `isPassword` omitted, `flagIncludeNotImportantViews`).
*   **Actions:** `AutomationCommand.Snapshot` / `Click` (`viewId/text/contentDescription/path`, `isClickable` ancestor `take 7`) / `SetText` (`isPassword` + `allowSensitiveInput false`, `ACTION_SET_TEXT`) / `Scroll` (`isScrollable` + `ACTION_SCROLL_FORWARD/BACKWARD`) / `GlobalAction` (`BACK/HOME/RECENTS/NOTIFICATIONS` via `performGlobalAction`) / `LaunchApp` (regex `^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$`, `FLAG_ACTIVITY_NEW_TASK`), all via `AgentRuntime.execute(call, userConfirmed)` + `AgentPolicy` (`ACCESSIBILITY_REQUIRED`).
*   **Shizuku:** `ShizukuController` (`READY_UID_2000` vs `PERMISSION_REQUIRED`/`UNAVAILABLE`), `PrivilegedUserService` (`AIDL` `IPrivilegedService`, `argv` allowlist `settings.get/device.info/package.*` etc., bounds, no `sh -c`), `ElevatedShell` (`UID 2000`, `timeout` `45s` watchdog, `install -r` keeps grant via `Shizuku` itself).
*   **Xiaomi:** HyperOS kills `Accessibility` on swipe-away / `Force Stop` — `AutomatorScreen` card `Lock 🔒 + No restrictions + Autostart` + `BackHandler` for `settingsVisible` + `install -r` keeps `persistedUriPermissions` (SAF) but not `Force Stop` — user must press `Home`, not `Force Stop`.

## Target

*   **Recipes:** Foreground-bound, per-step-confirmed `Task` (`core:pipeline` DAG) over `argv` allowlist, `loop`/`time` limits, `global stop` (`pin_to_little`), `persistent audit` (`ToolAuditLedger`), `service-death` recovery (`WeakReference` + `onDestroy` `detach`).
*   **Shizuku v1:** `scoped user recipes` (`argv` + `per-step confirmation` + `foreground binding` + `loop/time` + `global stop`, `install -r` grant) — same `argv` allowlist, no raw shell.
*   **Restrictions:** `MANAGE_EXTERNAL_STORAGE` never requested; `SAF` `ACTION_OPEN_DOCUMENT_TREE` only; `canTakeScreenshot` `11+` only; `OEM` `HyperOS` `AutoStart` + `Battery saver` handled; `canPerformGestures` via `Accessibility` only.

## Testing

`Accessibility` `connects` + `snapshot` `≤400`/`24` + `click/type/scroll/global/launch` selectors + `screenshot` `ARGB` + `Shizuku` `UID 2000` `READY` + `argv` injection (`package.force_stop` `;id`) + `install -r` grant persistence + `WorkManager` kill + `Force Stop` revocation (Xiaomi).
