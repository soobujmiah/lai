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
