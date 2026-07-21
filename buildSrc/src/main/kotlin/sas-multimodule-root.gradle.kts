plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    plugins.withId("java-test-fixtures") {
        configurations.named("testFixturesImplementation") {
            extendsFrom(configurations.named("implementation").get())
        }
        configurations.named("testImplementation") {
            extendsFrom(configurations.named("testFixturesImplementation").get())
        }
    }

    ktlint {
        ignoreFailures.set(true)
        filter {
            exclude { it.file.path.contains("generated") }
        }
    }

    dependencies {
        implementation(platform("io.ktor:ktor-bom:3.5.1"))
        implementation(platform("io.netty:netty-bom:4.2.16.Final"))
        implementation(platform("io.prometheus:prometheus-metrics-bom:1.8.0"))
        implementation(platform("tools.jackson:jackson-bom:3.2.1"))
        implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.1"))
        implementation(platform("org.eclipse.jetty:jetty-bom:12.1.11"))
        implementation(platform("org.eclipse.jetty.ee10:jetty-ee10-bom:12.1.11"))

        testImplementation(platform("org.junit:junit-bom:6.1.2"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation(kotlin("test"))

        constraints {
            // Rapids & Rivers drar transitivt inn sårbare versjoner på compileClasspath (runtime løftes
            // allerede via konfliktløsning, men compile beholder de laveste ønskede versjonene):
            //  - opentelemetry-api <= 1.61.0 (unbounded memory allocation i W3C Baggage-propagering).
            //    Løftes til nyeste versjon (patchet).
            //  - logback-core < 1.5.34 (deserialisering av utrygg data). Løftes til nyeste patchede versjon.
            implementation("io.opentelemetry:opentelemetry-api:1.64.0")
            implementation("ch.qos.logback:logback-core:1.5.38")
            implementation("ch.qos.logback:logback-classic:1.5.38")

            // WireMock drar transitivt inn handlebars < 4.5.2 med FileTemplateLoader path traversal (GHSA-6xhv-cwmr-6f52).
            // Sårbarheten ligger i selve handlebars-kjernen, så vi løfter kun den til nyeste patchede versjon.
            // handlebars-helpers holdes uendret: WireMock 3.13.2 trenger NumberHelper som ble fjernet i helpers 4.5.x.
            implementation("com.github.jknack:handlebars:4.5.3")

            // ktlint-pluginen drar inn logback-classic 1.3.14 -> logback-core med flere sårbarheter
            // (deserialisering av utrygg data, SSRF, EL-injection, arbitrær kodeeksekvering).
            // Løftes til nyeste patchede versjon på ktlint-konfigurasjonen.
            add("ktlint", "ch.qos.logback:logback-classic:1.5.38")
            add("ktlint", "ch.qos.logback:logback-core:1.5.38")
        }
    }
}

subprojects {
    kotlin {
        jvmToolchain(25)
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
                events("passed", "skipped", "failed")
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
