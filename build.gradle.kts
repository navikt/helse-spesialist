val junitJupiterVersion = "5.10.2"
val jvmTargetVersion = "21"
val graphqlKotlinVersion = "8.1.0"
val logbackSyslog4jVersion = "1.0.0"
val mockkVersion = "1.13.10"

plugins {
    kotlin("jvm") version "2.0.20"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
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
        implementation("com.papertrailapp:logback-syslog4j:$logbackSyslog4jVersion") // August, 2014
        {
            exclude(group = "ch.qos.logback")
        }

        constraints {
            implementation("io.netty:netty-all:4.1.108.final") {
                because("sårbarhet i 4.1.107.final")
            }
            implementation("org.apache.commons:commons-compress:1.26.0") {
                because("org.testcontainers:postgresql:1.19.7 -> 1.24.0 har en sårbarhet")
            }
            implementation("com.google.protobuf:protobuf-java:4.28.2") {
                because("com.expediagroup:graphql-kotlin-ktor-server:8.1.0 -> 4.27.1 har en sårbarhet")
            }
        }

        implementation("com.expediagroup:graphql-kotlin-client:$graphqlKotlinVersion")
        implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinVersion") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
        }
        implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlKotlinVersion") {
            exclude("com.expediagroup:graphql-kotlin-client-serialization")
        }
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
        testImplementation("io.mockk:mockk:$mockkVersion")
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
        register<Wrapper>("wrapper") {
            gradleVersion = "8.5"
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
