plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

allprojects {
    group = "no.nav.helse"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    ktlint {
        // Hvis du gjør endringer i disse filterne må du slette alle "build"/"out"-mappene og deretter
        // kjøre ./gradlew --no-build-cache ktlintCheck minst én gang for at endringene skal ta effekt
        filter {
            exclude { it.file.path.contains("generated") }
            exclude { it.file.path.contains("test") }
        }
    }

    dependencies {
        constraints {
            implementation("org.apache.commons:commons-compress:[1.27.1,)") {
                because("org.testcontainers:postgresql:1.19.7 -> 1.24.0 har en sårbarhet")
            }
            implementation("com.google.protobuf:protobuf-java:[4.31.1,)") {
                because("com.expediagroup:graphql-kotlin-ktor-server:8.3.0 -> 4.27.1 har en sårbarhet")
            }
            implementation("org.eclipse.jetty.http2:jetty-http2-common:12.0.25") {
                because(
                    "Eclipse Jetty HTTP/2 client can force the server to allocate a humongous byte buffer that may lead to OoM and subsequently the JVM to exit i versjon >= 12.0.0, <= 12.0.16",
                )
            }
        }

        testImplementation(platform("org.junit:junit-bom:6.0.2"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation(kotlin("test"))
        testImplementation("io.mockk:mockk:1.14.7")
    }
}

subprojects {
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.add("-Xexplicit-backing-fields")
        }
    }
    tasks {
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
