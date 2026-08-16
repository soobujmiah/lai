cmake_minimum_required(VERSION 3.22.1)
project(lai_runtime LANGUAGES CXX)

option(LAI_ENABLE_LLAMA_CPP "Compile the pinned llama.cpp adapter" OFF)
option(LAI_ENABLE_QNN "Compile the licensed QAIRT/QNN adapter" OFF)
set(LAI_LLAMA_CPP_DIR "" CACHE PATH "Path to a CI-fetched llama.cpp checkout")

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
        message(FATAL_ERROR "LAI_ENABLE_LLAMA_CPP=ON requires LAI_LLAMA_CPP_DIR")
    endif()
    message(FATAL_ERROR "Phase 1 exposes the JNI contract but intentionally does not claim a llama.cpp adapter. See docs/MODELS_AND_BACKENDS.md.")
endif()

if(LAI_ENABLE_QNN)
    message(FATAL_ERROR "Phase 1 does not ship proprietary QAIRT/QNN artifacts. See docs/MODELS_AND_BACKENDS.md.")
endif()
