plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.lai.runtime.platform.download"
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
    implementation(project(":core:policy"))
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.androidx.work.runtime)
    testImplementation(libs.junit)
}
