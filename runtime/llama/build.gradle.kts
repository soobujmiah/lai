plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.lai.runtime.runtime.llama"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-Wall", "-Wextra", "-Wpedantic")
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DLAI_ENABLE_LLAMA_CPP=${providers.gradleProperty("lai.enableLlamaCpp").orNull ?: "OFF"}",
                    // ggml-vulkan does find_package(SPIRV-Headers CONFIG REQUIRED). The apt-installed
                    // spirv-headers package puts its CMake config in /usr/share/cmake/SPIRV-Headers,
                    // but the NDK toolchain isolates package search to the NDK sysroot
                    // (CMAKE_FIND_ROOT_PATH_MODE_PACKAGE=ONLY), so the host config is invisible.
                    // Point at the exact config and allow host-prefix fallback. CI installs
                    // spirv-headers; local Vulkan builds must do the same.
                    "-DSPIRV-Headers_DIR=/usr/share/cmake/SPIRV-Headers",
                    "-DCMAKE_FIND_ROOT_PATH_MODE_PACKAGE=BOTH",
                )
                providers.gradleProperty("lai.llamaCppDir").orNull?.let {
                    arguments += "-DLAI_LLAMA_CPP_DIR=$it"
                }
                // Adreno OpenCL track (docs/BUILD_AND_RELEASE.md): CI fetches the pinned Khronos
                // OpenCL-Headers and builds the Khronos ICD loader as a static arm64 library; both
                // arrive as absolute paths. When both are present the native build compiles
                // ggml-opencl and links the loader into liblai_runtime.so.
                val openclIncludeDir = providers.gradleProperty("lai.openclIncludeDir").orNull
                val openclLibrary = providers.gradleProperty("lai.openclLibrary").orNull
                if (!openclIncludeDir.isNullOrBlank() && !openclLibrary.isNullOrBlank()) {
                    arguments += listOf(
                        "-DLAI_OPENCL_INCLUDE_DIR=$openclIncludeDir",
                        "-DLAI_OPENCL_LIBRARY=$openclLibrary",
                    )
                }
                // ggml-vulkan.cpp includes <vulkan/vulkan.hpp> and <spirv/unified1/spirv.hpp>,
                // which the NDK sysroot and apt packages do not expose to the cross-compiler
                // (apt installs them under /usr/include, hidden by NDK sysroot isolation). CI
                // fetches pinned KhronosGroup/Vulkan-Headers + SPIRV-Headers tags; inject both
                // include dirs for every C++ target (incl. the ggml-vulkan subdirectory).
                val extraIncludeDirs = listOfNotNull(
                    providers.gradleProperty("lai.vulkanHeadersDir").orNull?.let { "$it/include" },
                    providers.gradleProperty("lai.spirvHeadersDir").orNull?.let { "$it/include" },
                )
                if (extraIncludeDirs.isNotEmpty()) {
                    arguments += "-DCMAKE_CXX_STANDARD_INCLUDE_DIRECTORIES=${extraIncludeDirs.joinToString(";")}"
                }
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // kotlinOptions removed for AGP 9.3 bundled Kotlin
}

dependencies {
    api(project(":core:contracts"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
