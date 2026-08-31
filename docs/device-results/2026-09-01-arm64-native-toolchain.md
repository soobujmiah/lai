# ARM64 native toolchain validation — 2026-09-01

## Purpose

This record captures the minimal ARM64 JNI/native-library validation performed from `/tmp/real-device-test`. It is a tooling/reference test, not a backend-performance qualification.

## Environment evidence

- Target properties reported by ADB:
  - model: `25053RT47C`
  - ABI: `arm64-v8a`
  - Android: `16`
- ADB serial reported: `emulator-5554`.
- **Qualification note:** because the ADB serial is `emulator-5554`, this run must not be described as proof of physical-device execution. The model/ABI properties match the intended Redmi Turbo 4 Pro target, but the evidence is insufficient to distinguish a physical device from an emulator configuration. Physical-device qualification remains pending until a clearly identified physical ADB target is tested.

## Toolchain discovery

The repository-local Android SDK exists at:

```text
$HOME/android-sdk
```

Installed SDK components observed:

```text
build-tools/35.0.2
cmdline-tools/latest/bin/sdkmanager
platform-tools/adb
```

No NDK was initially installed. `sdkmanager --list` showed NDK `27.2.12479018` as available.

NDK `27.2.12479018` was installed successfully, but its host `clang-18` could not execute because this PRoot environment does not provide `/lib64/ld-linux-x86-64.so.2`. The NDK wrapper therefore could not be used directly.

The usable compiler was Termux Clang:

```text
/data/data/com.termux/files/usr/bin/clang
clang version 21.1.8
```

It accepts the Android target:

```bash
/data/data/com.termux/files/usr/bin/clang --target=aarch64-linux-android21 --version
```

## Canonical reusable build recipe

Run from `/tmp/real-device-test`:

```bash
cd /tmp/real-device-test

CLANG=/data/data/com.termux/files/usr/bin/clang

"$CLANG" \
  --target=aarch64-linux-android24 \
  -shared \
  -fPIC \
  -I"$PREFIX/include" \
  -o out/apk-root/lib/arm64-v8a/libarm64test.so \
  native/test.c \
  -llog

file out/apk-root/lib/arm64-v8a/libarm64test.so
readelf -h out/apk-root/lib/arm64-v8a/libarm64test.so | grep -E 'Class|Machine'
readelf -Ws out/apk-root/lib/arm64-v8a/libarm64test.so | grep '__android_log_print'
```

Expected evidence:

```text
ELF 64-bit LSB shared object, ARM aarch64
Class: ELF64
Machine: AArch64
__android_log_print ... GLOBAL ... UND ...
```

## Fresh APK assembly recipe

The project currently has no `build.sh`/`build-apk.sh`; the test APK can be assembled from the prepared APK root with the installed host tools:

```bash
cd /tmp/real-device-test

rm -f out/fresh-native-unsigned.apk \
      out/fresh-native-aligned.apk \
      out/fresh-native-signed.apk

cd out/apk-root
zip -q -r ../fresh-native-unsigned.apk .
cd ../..

ZIPALIGN="$HOME/android-sdk/build-tools/35.0.2/zipalign"
"$ZIPALIGN" -f -p 4 \
  out/fresh-native-unsigned.apk \
  out/fresh-native-aligned.apk

APKSIGNER=/usr/bin/apksigner
KEYSTORE=out/test.keystore

"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out out/fresh-native-signed.apk \
  out/fresh-native-aligned.apk

"$APKSIGNER" verify --verbose out/fresh-native-signed.apk
unzip -l out/fresh-native-signed.apk | grep 'lib/arm64-v8a/libarm64test.so'
```

The successful run produced a signed APK containing:

```text
lib/arm64-v8a/libarm64test.so
```

## Install / launch / native execution check

```bash
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.product.cpu.abilist

adb install -r out/fresh-native-signed.apk
adb shell am force-stop com.test.arm64device
adb logcat -c
adb shell monkey -p com.test.arm64device 1 >/dev/null
sleep 2
adb logcat -d -s ARM64NativeTest:I '*:S' | tail -10
```

Successful observed native log:

```text
ARM64NativeTest: ARM64 native code executed successfully
```

The process was observed running, and no `FATAL EXCEPTION`, `SIGSEGV`, `SIGABRT`, or `UnsatisfiedLinkError` was reported by the final crash check.

## What this proves

- The test source compiles into an AArch64 shared library using the available Termux Clang.
- The APK contains the library under the correct `arm64-v8a` path.
- Android installs the APK and selects `primaryCpuAbi=arm64-v8a` on the reported target.
- JNI loads and executes successfully.
- The native method reaches `__android_log_print` and returns its expected result.

## What this does NOT prove

- It does not prove that the Android NDK host binaries can execute inside the current PRoot environment.
- It does not prove Snapdragon 8s Gen 4 physical-device qualification because the ADB serial was `emulator-5554`.
- It does not qualify llama.cpp, Vulkan, OpenCL, QNN/HTP, or any production inference backend.
- It does not justify enabling an accelerator in the scheduler.

## Next gate

1. Connect and positively identify the actual physical Redmi Turbo 4 Pro through ADB; do not accept `emulator-5554` as physical evidence.
2. Repeat the same APK install/launch/native-log check on that physical target.
3. Only after that evidence is captured should the native test be promoted from tooling validation to physical-device native validation.
4. Keep this recipe as the repeatable fallback for this PRoot environment until a native NDK host-runtime solution is established.
