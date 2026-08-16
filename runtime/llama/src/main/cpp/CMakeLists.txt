cmake_minimum_required(VERSION 3.22.1)
project(lai_runtime LANGUAGES C CXX)

option(LAI_ENABLE_LLAMA_CPP "Compile the pinned llama.cpp CPU adapter" OFF)
set(LAI_LLAMA_CPP_DIR "" CACHE PATH "Path to a CI-fetched llama.cpp checkout")

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_library(
    lai_runtime
    SHARED
    native_inference.cpp
    backend_registry.cpp
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
    set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
    add_subdirectory("${LAI_LLAMA_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp" EXCLUDE_FROM_ALL)
    target_sources(lai_runtime PRIVATE llama_cpu_backend.cpp)
    target_compile_definitions(lai_runtime PRIVATE LAI_HAS_LLAMA_CPP=1)
    target_link_libraries(lai_runtime PRIVATE llama)
endif()
