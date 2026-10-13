# Native Android LAI — lessons from Debian FastRPC DSP qualification

**Date:** 2026-10-13  
**Scope:** reusable research and device evidence, not a LAI runtime change  
**LAI source inspected:** `main` at `4e883229c9c4361c1ccce6ac50bf9b969d3383f3`

## Purpose and native Android boundary

LAI is my native Android application. Its runtime uses Android/Bionic, JNI and
APK-packaged native libraries. My supplementary Debian PRoot experiment uses
Linux/glibc under Termux's Android sandbox. I record the experiment here because
its transport evidence and qualification method can inform LAI's Android Hexagon
work, not because LAI should adopt Debian libraries or a PRoot inference service.

My current LAI CPU response-correctness/device acceptance gate remains recorded
in `PROJECT_STATE.md`. This research adds no app implementation, Android build,
new installed artifact, default-backend selection or LAI inference validation.

## What I qualified outside LAI

I ran a standalone Debian Python/ctypes process on my Redmi Turbo 4 Pro
(SM8735, Android 16), using glibc libraries extracted into scratch storage.

| Stage | Classification | Exact evidence |
|---|---|---|
| Direct FastRPC node open | Failed | The Termux-owned process received errno 13; this is not a platform-wide NPU access verdict. |
| Android vendor FastRPC dlopen from glibc | Failed | Dependency resolution failed; a search-path retry trapped. This does not describe LAI's Bionic loader. |
| Binder broker lookup/session | Verified | API-36 `aidl6` service manager and `aidl3` RPC found the DSP service; `getNewDSPSession(0)` returned a manager. |
| Brokered CDSP descriptor | Verified | openSession domain 3 returned a NativeHandle; its duplicated FD resolved to `/dev/fastrpc-cdsp`. |
| Capability ioctls/CPU DMA memory | Verified | Capability queries worked; a 4 KiB DMA buffer allocated, CPU-mapped, cache-synced and passed a byte pattern roundtrip. |
| Unsigned DSP user PD/map/unmap | Verified | INIT_CREATE with attrs 8 and zero shell length succeeded; DMA mapping returned a nonzero DSP address, then unmap succeeded. |
| Application DSP kernel/file callback | Verified | Qualcomm's public calculator DSP ELF opened; the host callback opened its skeleton file. |
| Known-output DSP arithmetic | Verified | Direct remote sum([3,-1,7,11]) returned 20 and max returned 11, both status 0. |
| ggml v73 HTP/HMX or neural inference | Unverified here | No tensor kernel, ggml HTP library or model ran in this PRoot test. |

The calculator ELF is a **v68-targeted** public test binary running on my device's
CDSP; it is not LAI's `libggml-htp-v73.so`. Direct calculator APIs route through
`remote_handle64_invoke`; I did not use the host stub's local fallback/test helpers.
This is DSP arithmetic evidence, not a HMX/NPU tensor inference or speed claim.

The probe's process-local adapter gives Qualcomm's Linux FastRPC library duplicates
of the same process's legally brokered FD instead of reopening denied nodes. It
is a scratch diagnostic adapter, not an Android driver shipped by LAI. Last-handle
close deinitialized the library domain; broker session cleanup also passed.

Canonical raw findings, artifact identities, negative attempts and probe source:
[SKB dated evidence](https://github.com/soobujmiah/skb/blob/251b244/devices/redmi-turbo-4-pro/evidence/debian-proot-npu-investigation-2026-10-13.md).

## Lessons relevant to LAI's native Android implementation

1. **Separate direct-node failure from broker/runtime availability.** My glibc
   experiment confirms an authorized Binder-mediated path on this device. It
   does not prove LAI's app UID has identical service permissions, and it does
   not require replacing the Android vendor runtime. LAI's September Android-app
   Hexagon passes remain its stronger app-specific evidence.
2. **Choose host artifacts for the actual libc/platform.** The tested Debian
   libcdsprpc is glibc; LAI uses Bionic-compatible Android libraries and its
   namespace/manifest contract. The scratch LD_PRELOAD adapter is not a patch
   to import into the Android app. It is only a proven diagnostic technique.
3. **Require correct output after loading.** A device handle, DSP session or
   generated token count is weaker than a deterministic operation with checked
   output. Future LAI Hexagon qualification should separately prove operator
   output, placement/fallback and coherent model replies under the owner-operated
   Android test model. Historical September generation did not record reply sanity.
4. **Keep application packaging and transport gates distinct.** My read-only
   installed-APK inspection found no matching HTP/Hexagon archive entries. That
   APK is not a source for the old qualified kernel. Future Android qualification
   needs the exact build's packaged v73 ELF, namespace bridge and real DSP path,
   preserving the earlier AGP target/packaging fixes rather than assuming CMake
   configuration or backend compilation means the kernel reached the APK.
5. **Observe cleanup state before requesting another close.** My first calculator
   run computed correctly but its redundant session-close request failed after
   last-handle close had already deinitialized the domain. The corrected probe
   verifies library state and broker cleanup. Tooling cleanup errors must remain
   separate from compute correctness or app failures.
6. **Do not infer NPU operators or performance from this calculator.** The next
   relevant LAI experiment needs a compatible ggml v73 kernel, per-operation
   backend attribution, a compatible model and correct output. Existing Q4_K
   limits are tied to the recorded/pinned backend, not a universal future claim.

## Supplementary FastRPC and QAIRT/QNN research

**FastRPC ecosystem — verified metadata and bounded execution.** `fastrpc` is
Debian's source package; `libfastrpc1` contains the library. I actually ran
`libfastrpc1` 1.0.7-2~bpo13+1 arm64 through the broker adapter on this PRoot host.
Official metadata also lists `fastrpc-support` with support daemons/rules. I did
not install or run those daemons; package presence does not imply an unchanged
Android/PRoot deployment works. The tested callback library was sufficient for
this user-PD calculator run with Android's existing kernel and broker.

**QAIRT/QNN — exact combination unverified.** Qualcomm's documentation distinguishes
Android artifacts from conventional Linux toolchain artifacts. I did not select
or qualify a QAIRT release for **SM8735 + Android 16 + Debian/glibc**. The retrieved
support table did not establish this combination; that absence is not a claim
that SM8735 is unsupported. The vendor libQnnHtp ELF has Android/Bionic libc,
libm, libdl and liblog dependencies. I did not load it in this follow-up or copy
it into another environment. My earlier Local Dream QNN evidence concerns an
Android application and remains distinct from this Debian FastRPC result.

An exact QNN version still needs host-ABI compatibility, matching stub/skel and
firmware/API evidence, valid model/context compatibility, and a completed
correct-output HTP run. A working FastRPC calculator establishes transport and
DSP execution only. I deferred SDK downloads and runtime conversion work under
the supplementary research constraints.

Upstream references:

- [Qualcomm FastRPC](https://github.com/qualcomm/fastrpc)
- [Public calculator test/protocol context](https://github.com/qualcomm/fastrpc/blob/a56e9d4de3614a8d8f4b0e15ff561350c4b686e3/test/README.md)
- [Debian FastRPC package metadata](https://packages.debian.org/en/source/trixie-backports/fastrpc)
- [Qualcomm Neural Processing SDK](https://www.qualcomm.com/developer/software/neural-processing-sdk-for-ai)
- [QAIRT/QNN platform overview](https://docs.qualcomm.com/nav/home/QNN_general_overview.html?product=924033590759186372)
- [QNN target-platform artifact distinction](https://docs.qualcomm.com/nav/home/qnn_tutorial_windows_host_linux_target.html?product=924033590759186372)

## External Android QNN bridge reference: DroidRunner

I inspected DroidRunner's README, QNN service/client, HTP generation table,
verification flow and tests at upstream main commit
`5126eacf2855a252d38121238d787eacec6f3965`. Its native Android app runs QNN in
a separate `:qnn` process and lets its Linux PRoot runner request model work
through a loopback, per-job-token HTTP service. This is a useful fault-isolation
and measurement reference for a native Android project. It is not a dependency
or architecture decision for LAI, and no DroidRunner source was copied; its
repository is GPL-2.0-only.

The implementation pins QNN 2.49.0 artifacts, records which operators the QNN
delegate accepted, and supports explicit input/output files for comparing a
device result with a golden output. Its SoC-to-HTP table currently maps SM8550
to V73 and SM8650 to V75, but has no SM8735 entry. The README's measured QNN
example is from an SM8650/Snapdragon 8 Gen 3 phone. Thus this source confirms a
working Android-side QNN design pattern and a useful correctness contract, but
does not establish SM8735 compatibility. DroidRunner was not installed or run
on my device. LAI's active CPU correctness gate and existing native Android
state remain unchanged.

Source inspected:

- [DroidRunner at the inspected commit](https://github.com/m96-chan/DroidRunner/tree/5126eacf2855a252d38121238d787eacec6f3965)
- [HTP generation mapping](https://github.com/m96-chan/DroidRunner/blob/5126eacf2855a252d38121238d787eacec6f3965/app/src/main/java/io/github/m96chan/droidrunner/device/HexagonVersion.kt)
- [QNN client/service boundary](https://github.com/m96-chan/DroidRunner/blob/5126eacf2855a252d38121238d787eacec6f3965/app/src/main/java/io/github/m96chan/droidrunner/npu/QnnClient.kt)
- [QNN verification contract](https://github.com/m96-chan/DroidRunner/blob/5126eacf2855a252d38121238d787eacec6f3965/app/src/main/java/io/github/m96chan/droidrunner/npu/QnnVerification.kt)

## Standalone Android/Bionic QNN discovery — later 2026-10-13 follow-up

I built an independently written C diagnostic helper and invoked it from Debian
PRoot through Android's `/system/bin/linker64`, using the existing vendor
libraries in place. Local Dream was not required or installed in this test.
The source and exact sanitized discovery output are in
[Ternux's standalone experiment](https://github.com/soobujmiah/ternux/tree/main/experiments/qnn-native).

Both `/vendor/lib64/libQnnHtp.so` and `/vendor/lib64/libQnnSystem.so` loaded and
returned provider status 0, count 1. HTP reported core API **2.22.0**, backend
API **5.29.0**, and runtime build **v2.29.0.241129103708_105762**. The API-version
getter matched the public provider prefix; backend create returned status 0
with a handle, and backend free returned status 0. The helper was compiled
with existing Android-target Clang 21.1.8; its output is Bionic ARM64, not a
glibc library loaded into Debian. Provider discovery and backend initialization
are **device-validated for this helper only**. Later device/context results are
recorded below; graph execution and correct image generation remain unverified.

Useful lessons for later native LAI work:

- Record QNN **SDK/build identity** separately from **core/backend API versions**.
  A core API number of 2.22.0 does not mean the SDK is QNN 2.22.
- A helper launched in the Termux process context has a different linker/SELinux
  context from LAI's Android app. Vendor library load success here is not proof
  that LAI can load the same path. LAI needs its own packaged-runtime and device
  namespace qualification, preserving licensing and ABI boundaries.
- The existing `AbsoluteReality_qnn2.28_8gen2.zip` contains MNN CLIP, tokenizer
  and embedding files, and QNN UNet/VAE contexts. Its entries match the reference
  SD1.5 QNN layout, but its filename does not prove the compiled SoC or runtime
  compatibility. It stayed in place and was later streamed member-by-member into anonymous memory.
- Local Dream source at `647dc6f54b2859e4af59000746fde9c047627ff1` explicitly
  assigns SD1.5 CLIP to MNN CPU and UNet/VAE to QNN HTP. Do not label every
  image-generation stage as NPU execution. Its current build requests QAIRT
  2.50.0.260828; that is different from my observed vendor runtime 2.29.0.
- CPU/GPU inference needs compatible model/backend pairs; a compiled HTP
  context is not automatically a CPU/OpenCL model. Context loading, actual
  graph attribution, correct output and cleanup are separate gates.
- For Ternux I am pursuing my own standalone helper with bounded process IPC
  and a ZIP-member-to-anonymous-memory loader to preserve the original
  model path. The loader and VAE context test are implemented; image inference
  pipeline and known-output graph test are not.
  This is a research candidate for LAI, not an accepted LAI architecture.

### VAE context load through the standalone helper

The authorized QNN archive stayed at `/sdcard/Llms/AbsoluteReality_qnn2.28_8gen2.zip`.
Ternux's own `archive_input.py` streamed `vae_encoder.bin` into a sealed memfd;
CRC and SHA-256 were checked, with no model copy. QNN System API 1.2.0 read
compiled QNN 2.28.0 context metadata (core API 2.21.0, backend API 5.28.0).
The runtime is vendor QNN 2.29.0. The first HTP device create **failed** status
14001 because the existing vendor V73 stub was not loadable by the Termux
process's Android linker. Qualcomm's 2.29.0 Maven AAR has a V73 stub/skel;
two licensed files were placed in local scratch only, with no repository copy
or standalone redistribution. With those paths, backend/device/VAE context
create and explicit cleanup all returned status 0 on the physical SM8735.
The runtime log identified CDSP transport, the model graph and successful
context deserialization. An initial successful-load process crashed during HTP
`dlclose`; the short-lived helper retains the library through process exit and
its repeat run exited 0.

The owner clarified that PRoot Debian is the workstation. The first exploratory
helper was compiled through a Termux Android compiler path, so it is not a
portable package qualification. Ternux now has a pinned NDK GitHub build:
[run 38405297620](https://github.com/soobujmiah/ternux/actions/runs/38405297620)
passed and produced an ARM64/Bionic test artifact. The owner-mediated handoff
and physical-device repeat with that artifact are pending. No LAI app build
was involved.

For LAI, this is useful evidence that a compatible VAE HTP context can load
from an Android/Bionic process on this device. It does not prove graph execution,
correct tensors, image output, LAI app namespace access or a shippable runtime.
The public AAR license allows development/benchmarking use but prohibits
standalone redistribution; LAI packaging would need its own license review.
The next standalone test is fixed-input/fixed-output VAE graph execution with
matching QNN headers and per-stage HTP logs. See [Ternux's experiment](https://github.com/soobujmiah/ternux/tree/main/experiments/qnn-native)
and [Qualcomm's 2.29.0 POM](https://repo.maven.apache.org/maven2/com/qualcomm/qti/qnn-runtime/2.29.0/qnn-runtime-2.29.0.pom).

No LAI runtime, model catalog, Android manifest, backend default or APK was
changed. The owner requested recording these findings for later LAI work;
LAI's current CPU response-correctness and owner-operated app tests remain open.

References:

- [Public QNN interface API](https://docs.qualcomm.com/bundle/publicresource/topics/80-63442-10/api-rst_file_include_QNN_QnnInterface_h.html)
- [Public version types](https://docs.qualcomm.com/nav/home/api-rst_program_listing_file_include_QNN_QnnTypes_h.html?product=924033590759186372)
- [Reference stage assignment](https://github.com/xororz/local-dream/blob/647dc6f54b2859e4af59000746fde9c047627ff1/app/src/main/cpp/src/PipelineSd15Npu.hpp)
- [Reference SDK build](https://github.com/xororz/local-dream/blob/647dc6f54b2859e4af59000746fde9c047627ff1/app/src/main/cpp/CMakeLists.txt)

## Existing LAI evidence and next relevant gate

- [September Hexagon pass](../device-results/2026-09-04-redmi-turbo-4-pro-hexagon-htp-qualify-pass.md)
- [Reproducibility and recorded quantization boundary](../device-results/2026-09-04-redmi-turbo-4-pro-hexagon-reproducibility-and-quant-limit.md)
- [Runtime stabilization and response-correctness audit](../implementation/2026-10-04-runtime-stabilization-audit.md)
- [Current project state](../../PROJECT_STATE.md)

For PRoot, the remaining gate is an existing compatible v73 tensor/HTP kernel
and known-output neural execution. For native Android LAI, implementation and
qualification continue under its existing product state, Android artifact
handoff, packaging contracts and owner-operated test model. This note preserves
reusable research; it is not an ADR accepting a new architecture or a release gate
claim.

## Later standalone Bionic VAE execution evidence (2026-10-13)

An independently written helper built by Ternux's pinned Android NDK CI, then launched from Debian PRoot on the physical SM8735, loaded the existing QNN 2.28 VAE encoder and decoder contexts using Qualcomm's licensed QNN 2.29 V73 runtime files in scratch. Both graph handles were retrieved from CDSP and cleaned up with status 0. The encoder graph executed twice for the same raw-zero `uint16` input; output buffers changed and repeated bitwise (FNV64 `mean` `ae97b25a95bdac33`, `std` `7fb11db45bb92b10`). The model stayed in its original `/sdcard/Llms/` archive and was streamed to sealed anonymous FDs. [Ternux's experiment](https://github.com/soobujmiah/ternux/blob/main/experiments/qnn-native/README.md) and [SKB evidence](https://github.com/soobujmiah/skb/blob/main/devices/redmi-turbo-4-pro/evidence/debian-proot-npu-investigation-2026-10-13.md) retain reproduction details.

For LAI's future native Android work, this validates a Bionic QNN HTP/CDSP execution route on this phone outside the LAI app. It does **not** validate LAI's app UID/namespace, model integration, decoded tensor correctness, full image generation, or a correct ggml Q4 model answer. The immediately useful lesson is to keep graph execution, independent numerical oracle, and application integration as separate device gates. No LAI runtime code changed.
