plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.lai.runtime.runtime.llama"
    compileSdk = 35
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
                )
                providers.gradleProperty("lai.llamaCppDir").orNull?.let {
                    arguments += "-DLAI_LLAMA_CPP_DIR=$it"
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
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xjvm-default=all"
    }
}

dependencies {
    api(project(":core:contracts"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
