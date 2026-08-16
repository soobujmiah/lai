plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val releaseStoreFile = System.getenv("LAI_KEYSTORE_FILE")
val releaseStorePassword = System.getenv("LAI_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("LAI_KEY_ALIAS")
val releaseKeyPassword = System.getenv("LAI_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "dev.lai.runtime"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.lai.runtime"
        minSdk = 28
        targetSdk = 35
        versionCode = (providers.gradleProperty("lai.versionCode").orNull ?: "1").toInt()
        versionName = providers.gradleProperty("lai.versionName").orNull ?: "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("boolean", "PRODUCTION_SIGNED", hasReleaseSigning.toString())
    }

    flavorDimensions += "connectivity"
    productFlavors {
        create("connected") {
            dimension = "connectivity"
            buildConfigField("boolean", "AIR_GAPPED", "false")
        }
        create("airgap") {
            dimension = "connectivity"
            applicationIdSuffix = ".airgap"
            versionNameSuffix = "-airgap"
            buildConfigField("boolean", "AIR_GAPPED", "true")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xjvm-default=all"
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/LICENSE.md",
            "META-INF/NOTICE.md",
        )
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(project(":core:contracts"))
    implementation(project(":core:policy"))
    implementation(project(":core:scheduler"))
    implementation(project(":core:model"))
    implementation(project(":plugins:api"))
    implementation(project(":platform:download"))
    implementation(project(":platform:device"))
    implementation(project(":platform:accessibility"))
    implementation(project(":platform:shizuku"))
    implementation(project(":runtime:llama"))
    implementation(project(":runtime:ocr"))
    implementation(project(":runtime:orchestrator"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
