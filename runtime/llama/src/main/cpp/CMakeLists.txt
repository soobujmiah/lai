cmake_minimum_required(VERSION 3.22.1)
project(lai_runtime LANGUAGES C CXX)

option(LAI_ENABLE_LLAMA_CPP "Compile the pinned llama.cpp CPU adapter" OFF)
set(LAI_LLAMA_CPP_DIR "" CACHE PATH "Path to a CI-fetched llama.cpp checkout")
# Adreno OpenCL track: when CI provides the Khronos headers + a static ICD loader,
# ggml-opencl is compiled into liblai_runtime.so (docs/BUILD_AND_RELEASE.md).
set(LAI_OPENCL_INCLUDE_DIR "" CACHE PATH "Path to CI-fetched Khronos OpenCL-Headers")
set(LAI_OPENCL_LIBRARY "" CACHE FILEPATH "Path to CI-built static OpenCL ICD loader (libOpenCL.a)")
# Hexagon NPU track (docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md): when CI extracts the
# Hexagon SDK from the public ghcr.io/snapdragon-toolchain image, ggml-hexagon is compiled
# into liblai_runtime.so. Compile-only gate for now (no llama-hexagon backend selectable
# yet) -- see the handoff doc for the staged plan.
set(LAI_HEXAGON_SDK_ROOT "" CACHE PATH "Path to the extracted Hexagon SDK (e.g. .../hexagon/6.6.0.0)")
set(LAI_HEXAGON_TOOLS_ROOT "" CACHE PATH "Path to the Hexagon Tools compiler root inside the SDK")

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(
    lai_runtime
    SHARED
    native_inference.cpp
    backend_registry.cpp
    vulkan_backend.cpp
    opencl_backend.cpp
    hexagon_backend.cpp
    native_crash_handler.cpp
)

target_include_directories(lai_runtime PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/include")
target_compile_features(lai_runtime PRIVATE cxx_std_20)
target_compile_options(lai_runtime PRIVATE -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(lai_runtime PRIVATE -Wl,--gc-sections)
target_link_libraries(lai_runtime PRIVATE android log)

if(LAI_ENABLE_LLAMA_CPP)
    if(NOT EXISTS "${LAI_LLAMA_CPP_DIR}/CMakeLists.txt")
        message(FATAL_ERROR "LAI_ENABLE_LLAMA_CPP=ON requires a pinned LAI_LLAMA_CPP_DIR checkout")
    endif()
    set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_UI OFF CACHE BOOL "" FORCE)
    set(LLAMA_OPENSSL OFF CACHE BOOL "" FORCE)
    set(GGML_STATIC ON CACHE BOOL "" FORCE)
    set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
    set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
    set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
    set(GGML_CPU_KLEIDIAI ON CACHE BOOL "" FORCE)
    set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
    set(GGML_VULKAN ON CACHE BOOL "" FORCE)
    # Adreno OpenCL track. ggml-opencl's CMakeLists does find_package(OpenCL REQUIRED) and
    # find_package(Python3 REQUIRED); the hints below point at the CI-fetched Khronos
    # headers and the CI-built STATIC ICD loader, which gets linked into liblai_runtime.so
    # (no separate .so is packaged). At runtime the loader discovers the vendor Adreno
    # driver through /vendor/etc/OpenCL/drivers/*.icd, like every Android OpenCL app.
    if(EXISTS "${LAI_OPENCL_LIBRARY}" AND EXISTS "${LAI_OPENCL_INCLUDE_DIR}/CL/cl.h")
        set(GGML_OPENCL ON CACHE BOOL "" FORCE)
        set(GGML_OPENCL_EMBED_KERNELS ON CACHE BOOL "" FORCE)
        # Adreno-optimized matmul kernels are the point of this track; keep the default ON
        # explicit so a future ggml default change cannot silently drop them.
        set(GGML_OPENCL_USE_ADRENO_KERNELS ON CACHE BOOL "" FORCE)
        set(OpenCL_INCLUDE_DIR "${LAI_OPENCL_INCLUDE_DIR}" CACHE PATH "" FORCE)
        set(OpenCL_LIBRARY "${LAI_OPENCL_LIBRARY}" CACHE FILEPATH "" FORCE)
        message(STATUS "LAI: ggml-opencl enabled (Adreno track)")
    else()
        set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
        message(STATUS "LAI: ggml-opencl disabled (no OpenCL headers/ICD loader provided)")
    endif()
    # Hexagon NPU track. ggml-hexagon's CMakeLists does include(${HEXAGON_SDK_ROOT}/build/cmake/hexagon_fun.cmake)
    # and cross-compiles four HTP skels (v73/v75/v79/v81) via its own ExternalProject_Add sub-build with
    # its own toolchain file -- independent of this target's own NDK 27 build, so no version conflict
    # with the SDK's internally-expected NDK r25c. Compile-only gate: proves the CI SDK intake actually
    # lets ggml-hexagon build before any llama-hexagon backend is exposed to the Kotlin scheduler.
    if(EXISTS "${LAI_HEXAGON_SDK_ROOT}/build/cmake/hexagon_fun.cmake" AND EXISTS "${LAI_HEXAGON_TOOLS_ROOT}")
        set(GGML_HEXAGON ON CACHE BOOL "" FORCE)
        set(HEXAGON_SDK_ROOT "${LAI_HEXAGON_SDK_ROOT}" CACHE PATH "" FORCE)
        set(HEXAGON_TOOLS_ROOT "${LAI_HEXAGON_TOOLS_ROOT}" CACHE PATH "" FORCE)
        # Matches upstream's own docs/backend/snapdragon/CMakeUserPresets.json for the
        # arm64-android-snapdragon target exactly. Without this, hexagon_fun.cmake's own
        # string(FIND ...) calls receive an undefined (not just empty) variable, which CMake
        # drops as an argument entirely -- the exact "string sub-command FIND requires 3 or 4
        # parameters" error hit on the first real compile attempt.
        set(PREBUILT_LIB_DIR "android_aarch64" CACHE STRING "" FORCE)
        message(STATUS "LAI: ggml-hexagon enabled (compile-only gate, SDK at ${LAI_HEXAGON_SDK_ROOT})")
    else()
        set(GGML_HEXAGON OFF CACHE BOOL "" FORCE)
        message(STATUS "LAI: ggml-hexagon disabled (no Hexagon SDK provided)")
    endif()
    # Proper way: SPIRV-Headers + glslang are now installed on CI via workflow apt-get.
    add_subdirectory("${LAI_LLAMA_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp" EXCLUDE_FROM_ALL)
    target_sources(lai_runtime PRIVATE llama_cpu_backend.cpp llama_session.cpp)
    target_compile_definitions(lai_runtime PRIVATE LAI_HAS_LLAMA_CPP=1 LAI_HAS_VULKAN=1)
    if(GGML_OPENCL)
        target_compile_definitions(lai_runtime PRIVATE LAI_HAS_OPENCL=1)
    endif()
    if(GGML_HEXAGON)
        target_compile_definitions(lai_runtime PRIVATE LAI_HAS_HEXAGON=1)
    endif()
    target_link_libraries(lai_runtime PRIVATE llama)

    if(GGML_HEXAGON)
        # ggml-hexagon's HTP skels (v73/v75/v79/v81) are standalone DSP-side .so files built by
        # its own nested ExternalProject_Add sub-build with its own toolchain -- independent of
        # this target's NDK build (see the GGML_HEXAGON block above) -- so nothing links them
        # into lai_runtime and nothing places them where AGP packages native libs from. Copy them
        # there explicitly, using only CMake-resolved locations, never a guessed/hardcoded
        # .cxx/<hash> path: add_subdirectory() above pins llama.cpp's binary dir to
        # ${CMAKE_CURRENT_BINARY_DIR}/llama.cpp, and ExternalProject_Add's own default layout
        # (<name>-prefix/src/<name>-build/) is a fixed CMake convention, not an AGP-generated
        # hash -- confirmed against real CI output (run 33809644535) before writing this.
        # $<TARGET_FILE_DIR:lai_runtime> is the exact directory liblai_runtime.so itself lands
        # in, i.e. the same directory AGP's existing native-lib packaging already scans.
        set(LAI_HEXAGON_HTP_SKEL_DIR "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp/ggml/src/ggml-hexagon")
        set(LAI_HEXAGON_HTP_SKELS
            "${LAI_HEXAGON_HTP_SKEL_DIR}/htp-v73-prefix/src/htp-v73-build/libggml-htp-v73.so"
            "${LAI_HEXAGON_HTP_SKEL_DIR}/htp-v75-prefix/src/htp-v75-build/libggml-htp-v75.so"
            "${LAI_HEXAGON_HTP_SKEL_DIR}/htp-v79-prefix/src/htp-v79-build/libggml-htp-v79.so"
            "${LAI_HEXAGON_HTP_SKEL_DIR}/htp-v81-prefix/src/htp-v81-build/libggml-htp-v81.so"
        )
        # Ordering: lai_runtime's own build (and therefore this POST_BUILD step) cannot start
        # until all four HTP skel targets have finished, so the copy can never run early.
        add_dependencies(lai_runtime htp-v73 htp-v75 htp-v79 htp-v81)
        add_custom_command(
            TARGET lai_runtime POST_BUILD
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                ${LAI_HEXAGON_HTP_SKELS}
                "$<TARGET_FILE_DIR:lai_runtime>"
            COMMENT "LAI: copying ggml-hexagon HTP skels into lai_runtime's native-lib output dir"
            VERBATIM
        )
    endif()
endif()
