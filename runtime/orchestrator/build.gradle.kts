plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.lai.runtime.runtime.orchestrator"
    compileSdk = 37
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // kotlinOptions removed for AGP 9.3 bundled Kotlin
}

dependencies {
    api(project(":core:contracts"))
    implementation(project(":core:policy"))
    implementation(project(":platform:accessibility"))
    implementation(project(":platform:shizuku"))
    implementation(project(":runtime:ocr"))
    implementation(libs.kotlinx.serialization.json)
}
