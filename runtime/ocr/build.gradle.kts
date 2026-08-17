plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.lai.runtime.runtime.ocr"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:contracts"))
    implementation(libs.kotlinx.coroutines.android)
}
