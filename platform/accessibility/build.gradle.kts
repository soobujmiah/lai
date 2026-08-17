plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.lai.runtime.platform.accessibility"
    compileSdk = 37
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // kotlinOptions removed for AGP 9.3 bundled Kotlin
}

dependencies {
    implementation(project(":core:contracts"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
