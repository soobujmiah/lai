# ARM64 Native Tooling Guide

This is the reusable, short-path recipe for building and validating a small Android ARM64 JNI native library from the current Termux/PRoot environment.

> **Important:** this guide records the working path discovered during validation. Do not blindly substitute a different compiler/toolchain until the host-runtime check is understood.

## 1. Fast environment check

```bash
cd /tmp/real-device-test

ls -la "$HOME/android-sdk"
command -v sdkmanager || true
command -v adb
command -v zip
command -v apksigner
```

Expected SDK layout:

```text
$HOME/android-sdk/
├── build-tools/35.0.2/
├── cmdline-tools/latest/bin/sdkmanager
└── platform-tools/adb
```

## 2. Check/install Android NDK

Check first; do not run long filesystem searches unless this fails:

```bash
ls -la "$HOME/android-sdk/ndk" 2>/dev/null || echo "NO NDK DIRECTORY"

"$HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager" --list 2>/dev/null \
  | grep -E 'ndk;27\.2\.12479018|ndk;' \
  | head -20
```

If NDK 27.2 is required and absent:

```bash
"$HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager" "ndk;27.2.12479018"
```

Then:

```bash
NDK="$HOME/android-sdk/ndk/27.2.12479018"
ls -ld "$NDK"
test -x "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang" \
  && echo "ARM64 CLANG WRAPPER: OK" \
  || echo "ARM64 CLANG WRAPPER: MISSING"
```

## 3. NDK host-runtime caveat in PRoot

The NDK `clang-18` binary is an x86-64 Linux executable requesting:

```text
/lib64/ld-linux-x86-64.so.2
```

The current PRoot environment does not provide that interpreter. The observed failure is:

```text
cannot execute: required file not found
```

Therefore, **do not waste time repeatedly invoking the NDK wrapper** in this environment. Confirm the host-runtime condition once:

```bash
CLANG="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/clang"
ls -l "$CLANG"
file "$CLANG"
readelf -l "$CLANG" | grep -A1 'Requesting program interpreter'
ls -l /lib64/ld-linux-x86-64.so.2 2>/dev/null || echo "MISSING: /lib64/ld-linux-x86-64.so.2"
```

## 4. Working compiler: Termux Clang

The working compiler in this environment is:

```bash
CLANG=/data/data/com.termux/files/usr/bin/clang

"$CLANG" --version | head -3
"$CLANG" --target=aarch64-linux-android24 --version | head -3
```

Observed version during this validation:

```text
clang version 21.1.8
```

The Android target is accepted directly.

## 5. Build the ARM64 shared library

Source used by the test:

```text
native/test.c
```

Build:

```bash
cd /tmp/real-device-test

CLANG=/data/data/com.termux/files/usr/bin/clang

mkdir -p out/apk-root/lib/arm64-v8a

"$CLANG" \
  --target=aarch64-linux-android24 \
  -shared \
  -fPIC \
  -I"$PREFIX/include" \
  -o out/apk-root/lib/arm64-v8a/libarm64test.so \
  native/test.c \
  -llog
```

Verify immediately:

```bash
file out/apk-root/lib/arm64-v8a/libarm64test.so
readelf -h out/apk-root/lib/arm64-v8a/libarm64test.so \
  | grep -E 'Class|Machine'
readelf -Ws out/apk-root/lib/arm64-v8a/libarm64test.so \
  | grep '__android_log_print'
```

Expected:

```text
ELF 64-bit LSB shared object, ARM aarch64
Class: ELF64
Machine: AArch64
```

## 6. APK root layout

The native library must be packaged at exactly:

```text
out/apk-root/lib/arm64-v8a/libarm64test.so
```

Check:

```bash
find out/apk-root -maxdepth 3 -type f | sort
```

## 7. Assemble a fresh APK

The test project does not currently contain a build script, so this low-level assembly recipe is intentional:

```bash
cd /tmp/real-device-test

rm -f out/fresh-native-unsigned.apk \
      out/fresh-native-aligned.apk \
      out/fresh-native-signed.apk

cd out/apk-root
zip -q -r ../fresh-native-unsigned.apk .
cd ../..
```

Check the native library is present:

```bash
unzip -l out/fresh-native-unsigned.apk \
  | grep 'lib/arm64-v8a/libarm64test.so'
```

## 8. Zipalign

```bash
ZIPALIGN="$HOME/android-sdk/build-tools/35.0.2/zipalign"

test -x "$ZIPALIGN" && echo "ZIPALIGN: OK" || echo "ZIPALIGN: MISSING"

"$ZIPALIGN" -f -p 4 \
  out/fresh-native-unsigned.apk \
  out/fresh-native-aligned.apk
```

## 9. Sign and verify

For the test project:

```bash
APKSIGNER=/usr/bin/apksigner
KEYSTORE=out/test.keystore

"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out out/fresh-native-signed.apk \
  out/fresh-native-aligned.apk

"$APKSIGNER" verify --verbose out/fresh-native-signed.apk
```

**Do not reuse these test keystore credentials for production signing.**

## 10. Check the final APK

```bash
unzip -l out/fresh-native-signed.apk \
  | grep 'lib/arm64-v8a/libarm64test.so'

ls -lh out/fresh-native-signed.apk
```

## 11. Identify the ADB target before installing

Always run this first:

```bash
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.product.cpu.abilist
adb shell getprop ro.build.version.release
```

**Important:** an ADB serial such as `emulator-5554` must not be treated as proof of physical hardware. For physical-device qualification, positively identify the actual phone and record the evidence.

## 12. Install and run

```bash
adb install -r out/fresh-native-signed.apk
adb shell am force-stop com.test.arm64device
adb logcat -c
adb shell monkey -p com.test.arm64device 1 >/dev/null
sleep 2
adb logcat -d -s ARM64NativeTest:I '*:S' | tail -10
```

Successful native execution should produce:

```text
ARM64NativeTest: ARM64 native code executed successfully
```

## 13. Final crash check

```bash
adb logcat -d \
  | grep -E 'FATAL EXCEPTION|SIGSEGV|SIGABRT|UnsatisfiedLinkError' \
  | tail -20 \
  || echo "NO NATIVE/JAVA CRASH DETECTED"
```

## 14. Minimal repeat checklist

When repeating this experiment, use this order:

1. `ls $HOME/android-sdk` — confirm SDK.
2. Check NDK directory — do not scan the whole filesystem.
3. If NDK exists, test host execution once.
4. If NDK host execution fails because the loader is missing, use Termux Clang.
5. Build `arm64-v8a/lib*.so`.
6. `file` + `readelf` verify ELF architecture.
7. Package into APK.
8. `zipalign`.
9. `apksigner`.
10. Verify APK contains `lib/arm64-v8a/`.
11. Identify ADB target.
12. Install → launch → logcat.
13. Record evidence.

This order deliberately puts cheap, high-information checks before expensive searches/downloads.

## 15. Scope boundary

This guide is a **tooling and JNI smoke-test recipe**. A successful run does not qualify Vulkan, OpenCL, QNN/HTP, llama.cpp acceleration, or production inference. Those require their own evidence and device-specific qualification records.
