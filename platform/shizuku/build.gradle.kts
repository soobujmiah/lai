plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.lai.runtime.platform.shizuku"
    compileSdk = 36
    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // kotlinOptions removed for AGP 9.3 bundled Kotlin
}

dependencies {
    implementation(project(":core:contracts"))
    implementation(project(":core:policy"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
