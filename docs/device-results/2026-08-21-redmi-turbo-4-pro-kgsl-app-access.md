# Redmi Turbo 4 Pro — LAI app-context KGSL access qualification

**Date:** 2026-08-21  
**Package:** `dev.lai.runtime.debug`  
**Purpose:** establish whether the stock Android app identity can reach the Qualcomm KGSL device node before attempting GPU runtime qualification.

## Device

- Xiaomi Redmi Turbo 4 Pro (`25053RT47C`), codename `onyx`
- Android 16
- QTI `SM8735` / Snapdragon 8s Gen 4
- Adreno 825
- Vendor SDK 35
- Platform `sun`

## LAI process identity

```text
package: dev.lai.runtime.debug
PID: 19426
UID/GID: 10675
SELinux: u:r:untrusted_app:s0:c163,c258,c512,c768
```

## Kernel/device evidence

```text
GPU model: Adreno825
Kernel module: msm_kgsl (loaded)
Device: /dev/kgsl-3d0
Mode: 0666
SELinux type: gpu_device
```

Shell-side baseline:

```text
exec 3<>/dev/kgsl-3d0
=> exit status 0
=> KGSL OPEN: YES
```

## LAI UID qualification

`run-as dev.lai.runtime.debug id` returned UID 10675 with the expected application-associated context.

`run-as dev.lai.runtime.debug ls -lZ /dev/kgsl-3d0` confirmed the node is visible.

Read/write tests:

```text
READ: YES
WRITE: YES
```

Actual open test:

```text
exec 3<>/dev/kgsl-3d0
=> LAI KGSL OPEN: YES
```

## Result

**RAW KGSL DEVICE-NODE ACCESS: AVAILABLE.**

The normal Android app UID/SELinux boundary does not prevent the LAI debug identity from opening the Qualcomm KGSL character device on this build.

This is **not** equivalent to GPU support. It does not prove that a GPU command queue can be created, that OpenCL can be loaded, that Vulkan is stable, or that an LLM can execute on the GPU.

It does, however, invalidate the earlier broad hypothesis that LAI cannot reach the GPU kernel interface because it is an ordinary Android application.

## Important distinction from the OpenCL result

The existing OpenCL investigation found that the normal modern-app linker namespace refuses `libOpenCL.so`. That remains a valid result for the current ggml-opencl loading route.

The new KGSL result only says that the underlying kernel device node is accessible. It justifies a new, controlled native investigation of Qualcomm's userspace GPU runtimes; it does not assert that Android's OpenCL linker wall has been bypassed.

Also, executing `/vendor/lib64/libOpenCL_adreno.so` or `/vendor/lib64/hw/vulkan.adreno.so` directly is not a valid test. These are shared libraries and must be loaded by a process using normal dynamic-loader semantics.

## Next experiments

### A. Minimal native KGSL probe

Create a diagnostic-only native probe that:

1. calls `open("/dev/kgsl-3d0", O_RDWR | O_CLOEXEC)`;
2. records success/errno;
3. performs only documented/safe read-only driver queries where available;
4. closes the descriptor;
5. exports the result through LAI diagnostics.

### B. OpenCL loader probe

From the same LAI native process:

1. `dlopen()` the Qualcomm OpenCL library using normal Android linker rules;
2. resolve `clGetPlatformIDs` and `clGetDeviceIDs`;
3. enumerate platforms/devices;
4. record vendor/device/version/extensions;
5. if a GPU device is returned, execute a tiny matrix/vector sanity workload;
6. only after that consider integrating the result with llama.cpp.

### C. Vulkan qualification

Separately apply the documented ggml-vulkan warptile clamp experiment, build one qualification APK, capture the actual subgroup size, and compare the result against the previous `vkCmdBindPipeline` crash.

### D. QNN/Hexagon

Keep QNN/Hexagon as a separate acceleration track. Determine the SM8735 HTP/DSP architecture and the real app-side loading path before building a production adapter.

## Safety / project policy

Do not disable SELinux, modify `/vendor`, lower targetSdk, or introduce security bypasses merely to make a qualification pass. The objective is to discover what stock Android permits and build supported paths around that evidence.

## Status transition

`UNKNOWN app GPU-device access` → **`AVAILABLE raw KGSL device access`**.

Next status can become `SUPPORTED` only after successful GPU computation and repeatable device evidence.
