plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:contracts"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
