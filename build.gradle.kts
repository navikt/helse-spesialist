plugins {
    alias(libs.plugins.kotlin.jvm)
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

allprojects {
    group = "no.nav.helse"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        ignoreFailures.set(true)
        // Hvis du gjør endringer i disse filterne må du slette alle "build"/"out"-mappene og deretter
        // kjøre ./gradlew --no-build-cache ktlintCheck minst én gang for at endringene skal ta effekt
        filter {
            exclude { it.file.path.contains("generated") }
        }
    }

    dependencies {
        implementation(platform("io.prometheus:prometheus-metrics-bom:1.7.0"))
        implementation(platform("tools.jackson:jackson-bom:3.2.1"))

        testImplementation(platform("org.junit:junit-bom:6.1.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.14.11")
    }
}

subprojects {
    kotlin {
        jvmToolchain(21)
    }
    tasks {
        // Ikke skriv ut feilformatert kode under bygging
        withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask>().configureEach {
            enabled = false
        }
        // Kjør formatering
        named<Task>("check") {
            dependsOn("ktlintFormat")
        }

        named<Test>("test") {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
                showStackTraces = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}

tasks {
    jar {
        enabled = false
    }
    build {
        val erCiBygg = providers.environmentVariable("GITHUB_ACTIONS").orNull == "true"
        if (!erCiBygg) {
            dependsOn("addKtlintFormatGitPreCommitHook")
        }
    }
}
