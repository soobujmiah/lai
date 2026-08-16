pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lai"

// Composition root / product shell
include(":app")

// Pure JVM contracts and decision logic: no Android, network, JNI, or vendor APIs.
include(":core:contracts")
include(":core:policy")
include(":core:scheduler")
include(":core:model")
include(":plugins:api")

// Android authority and private persistence boundaries.
include(":platform:download")
include(":platform:audit")
include(":platform:device")
include(":platform:accessibility")
include(":platform:shizuku")

// Replaceable intelligence/runtime adapters.
include(":runtime:llama")
include(":runtime:ocr")
include(":runtime:orchestrator")
