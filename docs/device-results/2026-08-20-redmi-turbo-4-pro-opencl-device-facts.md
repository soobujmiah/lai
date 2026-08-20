# Redmi Turbo 4 Pro — Adreno 825 OpenCL device facts (collected via Shizuku shell)

Collection date: 2026-08-20 (16:32 Asia/Dhaka)
Collected from: Shizuku `rish` interactive shell (uid 2000), build 0.1.190 session
Purpose: one-time capture of the device's OpenCL layout so vendor-discovery work never
needs re-collection over adb. Treat these as the ground truth for the Adreno OpenCL track
(docs/BUILD_AND_RELEASE.md § "GPU enablement — Adreno OpenCL track").

## Device identity

| Field | Value |
|---|---|
| Model | Xiaomi 25053RT47C (Redmi Turbo 4 Pro), device codename `onyx` |
| OS | Android 16, kernel `6.6.77-android15-8-g4a507830d890-ab13636` |
| SoC | QTI SM8735 (Snapdragon 8s Gen 4), Adreno 825 |
| RAM | 12 GB (11233 MiB usable) |

## OpenCL layout — what EXISTS on this device

- `/vendor/lib64/libOpenCL.so` — **present**, 95,800 bytes, `-rw-r--r-- root:root`.
  Small size indicates Qualcomm's shim/loader library; the real driver hangs off it.
- `/vendor/etc/public.libraries.txt` — **contains `libOpenCL.so`** (alongside
  `libadsprpc.so`, `libcdsprpc.so`, `libsdsprpc.so`, `libfastcvopt.so`, `libSNPE.so`,
  Xiaomi camera libs). **Consequence: an app process CAN `dlopen("libOpenCL.so")`**
  through the public-library namespace — this is the canonical path for OpenCL on this
  device.
- `/system/lib64/libOpenCL.so` — absent.

## OpenCL layout — what does NOT exist on this device

- No `.icd` vendor files anywhere readable under `/vendor/etc` (find run as shell uid;
  a few `hal_uuid_map_*.xml` and SELinux contexts files deny even shell, but no OpenCL
  drivers/vendors directory surfaced).
- `/vendor/Khronos/OpenCL/vendors` — **does not exist** (this is the ONLY default search
  path of the Khronos ICD loader on Android per `icd_platform.h` — the reason the
  statically linked loader found zero platforms in builds #188/#190).
- `/system/vendor/Khronos/OpenCL/vendors` — does not exist either.

## Diagnostic lines that prove each layer (from `logcat -s LAI-llama`)

| Line | Meaning |
|---|---|
| `opencl: dlopen(libOpenCL.so) OK` | public-library namespace exposes the vendor driver to the app |
| `opencl: dlopen(libOpenCL.so) failed: …` | linker-namespace problem (names the exact error) |
| `opencl: no system ICD directory — synthesized 4 vendor entries at …` | LAI vendor-directory synthesis ran |
| `opencl probe: device N type=… name='GPUOpenCL' description='Adreno (TM) 825'` | ggml registered the OpenCL GPU device — backend available |
| `opencl: compiled but no OpenCL GPU device registered` | ICD loader enumerated zero platforms |
| `device: pinned offload to 'GPUOpenCL' (+ CPU for the remainder)` | model load targets the Adreno device |

## Session history (build → finding)

- **#188** (`e210732`): OpenCL compiled+linked (verified by artifact symbol inspection),
  probe found zero platforms — Khronos ICD loader default path empty on this device.
- **#190** (`fda2c24`): vendor-directory synthesis shipped, but logcat showed
  `opencl: GGML_OPENCL not compiled` — root cause was NOT the device: a workspace
  snapshot regression had silently rolled back `.github/workflows/android_build.yml` in
  `fda2c24` (the OpenCL fetch/build CI step was missing), so the build compiled without
  ggml-opencl. Workflow restored from `e210732` in the follow-up commit; see
  CURRENT_STATUS snapshot notes.

## Next evidence needed (one app restart + `logcat -d -s LAI-llama | grep -i opencl`)

With a build that actually contains ggml-opencl again: the table above names the exact
remaining stage if anything fails (dlopen vs ICD compliance vs probe).

## Update 2 — build #192 probe results (2026-08-20 17:13)

Build #192 was the first artifact that actually contained ggml-opencl after the CI
restoration. Its probe ran exactly as designed and produced the decisive sequence:

```text
opencl: no system ICD directory — synthesized 4 vendor entries at /data/user/0/dev.lai.runtime/files/lai-opencl-vendors (OCL_ICD_VENDORS set)
ggml_opencl: platform IDs not available.
opencl: dlopen(libOpenCL.so) failed: dlopen failed: library "libOpenCL.so" not found
opencl probe: device 0 type=2 name='Vulkan0' description='Adreno (TM) 825'
opencl probe: device 1 type=0 name='CPU' description='CPU'
opencl: compiled but no OpenCL GPU device registered (no vendor ICD?)
```

Interpretation:

- Vendor-directory synthesis and OCL_ICD_VENDORS plumbing WORKED (all four .icd
  candidates were attempted by the ICD loader).
- **The wall is Android itself: `dlopen("libOpenCL.so")` from the app process returns
  "library not found" even though `/vendor/lib64/libOpenCL.so` exists and is listed in
  `/vendor/etc/public.libraries.txt`.** The bionic linker reports both ENOENT and EACCES
  (SELinux denial) as "library not found", so the two remaining hypotheses are:
  1. SELinux policy denies untrusted_app access to the vendor OpenCL library
     (a known HyperOS/Xiaomi behaviour — Termux/clinfo users hit it on this vendor);
  2. the linker namespace config on Android 16 does not expose it to apps targeting SDK 36.
- Consequence: any approach that dlopens the vendor lib FROM THE APP inherits this wall —
  including absolute-path .icd candidates (they failed identically via the ICD loader).

Next evidence gates (record results here):

- [ ] OpenCL-Z (Play Store) on this exact device: does it show the Adreno 825 OpenCL
      platform? YES → an app-visible path exists (find it via linker debug logs);
      NO → Xiaomi blocks OpenCL for third-party apps on this device, and the OpenCL track
      closes here (pivot: wait for Vulkan driver fix, or QNN/HTP later).
- [ ] Linker debug trace: `setprop debug.ld.app dlopen` (in rish), restart LAI,
      `logcat -d | grep -i linker` — shows the exact search paths / denial.

## Final verdict — OpenCL track CLOSED on this device (device-policy wall)

Evidence chain completed 2026-08-20:

1. **OpenCL-Z (legacy 2015 app, targetSdk ~22) sees the full stack:** platform
   `QUALCOMM Snapdragon(TM)`, `OpenCL 3.0 QUALCOMM build: 0800.33`, device
   `QUALCOMM Adreno(TM) 825`, FULL_PROFILE, compiler available, 8 compute units, unified
   memory, 5.5 GB global, `cl_khr_subgroups`/`cl_qcom_dot_product8`/`cl_khr_bfloat16`
   among extensions. Loaded via **32-bit** `/system/vendor/lib/libOpenCL.so`. Report:
   `OpenCL-Z-Android-Report.txt` (kept outside the repo per screenshot policy; facts above
   are the retained text evidence).
2. **LAI (targetSdk 36) is refused by the linker** (debug.ld.app=dlopen trace):
   `library "/vendor/lib64/libOpenCL.so" … is not accessible for the namespace
   [clns-10, permitted_paths="/data:/mnt/expand:/data/user/0/dev.lai.runtime"]`.
3. **`cat /linkerconfig/ld.config.txt | grep -n -i opencl` → EMPTY.** The generated
   linker configuration publishes `libOpenCL.so` to NO app namespace on this HyperOS
   build (`BP2A.250605.031.A3`), even though `/vendor/etc/public.libraries.txt` lists
   it — legacy apps bypass that config entirely, which is the only reason OpenCL-Z works.

**Conclusion:** the Adreno 825 OpenCL stack is fully functional; HyperOS restricts it to
legacy-targeting apps. No app-side code can honestly reach it from a modern targetSdk.
The OpenCL backend stays compiled and dormant: it will self-activate (probe → scheduler
evidence → offload, zero code change) if a future HyperOS build publishes the library to
app namespaces. Deliberately lowering targetSdk to bypass the linker namespace violates
the project's security policy and Play requirements, and is rejected.

GPU acceleration status after this session:

| Path | State |
|---|---|
| Vulkan | driver bug at `vkCmdBindPipeline` (MUL_MAT bind) — retest on Qualcomm driver updates |
| OpenCL | device-policy wall (this record) — dormant backend auto-activates if Xiaomi publishes the library |
| QNN/HTP (NPU) | the remaining sanctioned path: Qualcomm distributes the QNN runtime for bundling INSIDE the APK, which bypasses the linker wall entirely; needs licensed QAIRT SDK + model conversion (roadmap) |
| CPU | device-validated baseline; remains the shipped default |
