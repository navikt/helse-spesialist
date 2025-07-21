plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
}

val githubUser: String by project
val githubPassword: String by project

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/*")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        ignoreFailures = true
        // Hvis du gjør endringer i disse filterne må du slette alle "build"/"out"-mappene og deretter
        // kjøre ./gradlew --no-build-cache ktlintCheck minst én gang for at endringene skal ta effekt
        filter {
            exclude { it.file.path.contains("generated") }
            exclude { it.file.path.contains("test") }
        }
    }

    dependencies {
        constraints {
            implementation("net.minidev:json-smart:[2.5.2,)") {
                because("Sårbarhet CVE-2024-57699")
            }
            implementation("org.apache.commons:commons-compress:[1.27.1,)") {
                because("org.testcontainers:postgresql:1.19.7 -> 1.24.0 har en sårbarhet")
            }
            implementation("com.google.protobuf:protobuf-java:[4.31.1,)") {
                because("com.expediagroup:graphql-kotlin-ktor-server:8.3.0 -> 4.27.1 har en sårbarhet")
            }
            implementation("io.micrometer:micrometer-registry-prometheus:[1.15.1,)") {
                because("com.github.navikt:rapids-and-rivers avhenger av micrometer-registry-prometheus:1.14.5")
            }
            implementation("org.apache.kafka:kafka-clients:[3.9.1,)") {
                because("Apache Kafka Client Arbitrary File Read and Server Side Request Forgery Vulnerability i versjon >= 3.1.0, < 3.9.1")
            }
            implementation("org.eclipse.jetty.http2:jetty-http2-common:12.0.23") {
                because(
                    "Eclipse Jetty HTTP/2 client can force the server to allocate a humongous byte buffer that may lead to OoM and subsequently the JVM to exit i versjon >= 12.0.0, <= 12.0.16",
                )
            }
        }

        testImplementation(platform("org.junit:junit-bom:5.13.4"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.14.5")
    }
}

subprojects {
    kotlin {
        jvmToolchain(21)
    }
    tasks {
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
    wrapper {
        gradleVersion = "8.14.3"
    }
    jar {
        enabled = false
    }
    build {
        doLast {
            val erLokaltBygg = !System.getenv().containsKey("GITHUB_ACTION")
            val manglerPreCommitHook = !File(".git/hooks/pre-commit").isFile
            if (erLokaltBygg && manglerPreCommitHook) {
                println(
                    """
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ¯\_(⊙︿⊙)_/¯ !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    !            Hei du! Det ser ut til at du mangler en pre-commit-hook :/         !
                    ! Du kan installere den ved å kjøre './gradlew addKtlintFormatGitPreCommitHook' !
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    """.trimIndent(),
                )
            }
        }
    }
}
