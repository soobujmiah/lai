plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.lai.runtime.platform.workspace"
    compileSdk = 36
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
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
