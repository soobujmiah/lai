cmake_minimum_required(VERSION 3.22.1)
project(lai_runtime LANGUAGES C CXX)

option(LAI_ENABLE_LLAMA_CPP "Compile the pinned llama.cpp CPU adapter" OFF)
set(LAI_LLAMA_CPP_DIR "" CACHE PATH "Path to a CI-fetched llama.cpp checkout")
# Adreno OpenCL track: when CI provides the Khronos headers + a static ICD loader,
# ggml-opencl is compiled into liblai_runtime.so (docs/BUILD_AND_RELEASE.md).
set(LAI_OPENCL_INCLUDE_DIR "" CACHE PATH "Path to CI-fetched Khronos OpenCL-Headers")
set(LAI_OPENCL_LIBRARY "" CACHE FILEPATH "Path to CI-built static OpenCL ICD loader (libOpenCL.a)")

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(
    lai_runtime
    SHARED
    native_inference.cpp
    backend_registry.cpp
    vulkan_backend.cpp
    opencl_backend.cpp
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
    set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)
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
    # Proper way: SPIRV-Headers + glslang are now installed on CI via workflow apt-get.
    add_subdirectory("${LAI_LLAMA_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp" EXCLUDE_FROM_ALL)
    target_sources(lai_runtime PRIVATE llama_cpu_backend.cpp llama_session.cpp)
    target_compile_definitions(lai_runtime PRIVATE LAI_HAS_LLAMA_CPP=1 LAI_HAS_VULKAN=1)
    if(GGML_OPENCL)
        target_compile_definitions(lai_runtime PRIVATE LAI_HAS_OPENCL=1)
    endif()
    target_link_libraries(lai_runtime PRIVATE llama)
endif()
