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
                // AGP's CMake integration only builds targets that are link-dependencies of the
                // module's own .so output (lai_runtime) unless told otherwise -- confirmed via
                // --info build log: "not building target htp-v73 because no targets are
                // specified and library build output file is null" for all four HTP skel
                // libraries. They are standalone DSP-side shared objects (loaded by the Hexagon
                // FastRPC skel loader via ADSP_LIBRARY_PATH, never linked into lai_runtime), so
                // CMake's own dependency graph never pulls them in as a side effect of building
                // lai_runtime the way the static ggml/llama libs are. Must list every .so target
                // actually needed in the APK explicitly. See
                // docs/device-results/2026-09-03-redmi-turbo-4-pro-hexagon-session-open-diagnosis.md.
                // The four HTP skel targets only exist in the CMake project when GGML_HEXAGON is
                // ON (see CMakeLists.txt); listing them unconditionally here breaks every build
                // variant that doesn't enable Hexagon ("Unexpected native build target htp-v75",
                // observed in CI run 33790660621). They're appended below, gated on the same
                // lai.hexagonSdkRoot/lai.hexagonToolsRoot check that flips GGML_HEXAGON itself.
                targets += "lai_runtime"
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
                // Hexagon NPU track (docs/HANDOFF-2026-09-03-npu-hexagon-scoping.md): CI extracts
                // just the Hexagon SDK subtree from the public snapdragon-toolchain image. Compile-only
                // gate for now -- see runtime/llama/src/main/cpp/CMakeLists.txt.
                val hexagonSdkRoot = providers.gradleProperty("lai.hexagonSdkRoot").orNull
                val hexagonToolsRoot = providers.gradleProperty("lai.hexagonToolsRoot").orNull
                if (!hexagonSdkRoot.isNullOrBlank() && !hexagonToolsRoot.isNullOrBlank()) {
                    arguments += listOf(
                        "-DLAI_HEXAGON_SDK_ROOT=$hexagonSdkRoot",
                        "-DLAI_HEXAGON_TOOLS_ROOT=$hexagonToolsRoot",
                    )
                    // GGML_HEXAGON only turns ON in CMakeLists.txt under this same condition;
                    // the HTP skels only exist as CMake targets when it does.
                    targets += listOf("htp-v73", "htp-v75", "htp-v79", "htp-v81")
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
            // Must match the pinned CMake installed by the "Install pinned CMake" workflow step
            // (cmake.dir in local.properties) -- ggml-hexagon's htp/CMakeLists.txt requires
            // >=3.22.2, which the SDK-managed cmake;3.22.1 (still installed above, unused for
            // this exact-version request) does not satisfy.
            version = "3.31.6"
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
