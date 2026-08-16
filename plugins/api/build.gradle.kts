plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:contracts"))
    api(project(":core:policy"))
    testImplementation(libs.junit)
}
