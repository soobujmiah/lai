import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Pure-JVM logic is fast to test remotely and carries a per-module coverage ratchet.
val coverageFloors = mapOf(
    ":core:contracts" to 0.15,
    ":core:policy" to 0.55,
    ":core:scheduler" to 0.70,
    ":plugins:api" to 0.50,
)
val coverageCheck = tasks.register("coverageCheck") {
    group = "verification"
    description = "Runs pure-JVM tests and prevents module coverage regression."
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        apply(plugin = "jacoco")
        extensions.configure<JacocoPluginExtension> { toolVersion = "0.8.13" }
        tasks.withType<Test>().configureEach {
            extensions.configure<JacocoTaskExtension> { isIncludeNoLocationClasses = false }
            finalizedBy(tasks.named("jacocoTestReport"))
        }
        tasks.named<JacocoReport>("jacocoTestReport") {
            dependsOn(tasks.named("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }
        val floor = coverageFloors[path] ?: 0.50
        val verify = tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
            dependsOn(tasks.named("test"))
            violationRules {
                rule {
                    limit {
                        counter = "LINE"
                        value = "COVEREDRATIO"
                        minimum = floor.toBigDecimal()
                    }
                }
            }
        }
        coverageCheck.configure { dependsOn(verify) }
    }
}
