val junitJupiterVersion = "5.7.2"
val ktorVersion = "1.6.2"

plugins {
    kotlin("jvm") version "1.5.21"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:2021.07.08-10.12.37eff53b5c39")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("com.papertrailapp:logback-syslog4j:1.0.0") //August, 2014
        implementation("com.zaxxer:HikariCP:5.0.0")
        implementation("no.nav:vault-jdbc:1.3.7")
        implementation("org.flywaydb:flyway-core:7.13.0")
        implementation("com.github.seratch:kotliquery:1.3.1") //April, 2019
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        implementation("io.ktor:ktor-client-jackson:$ktorVersion")
        implementation("io.ktor:ktor-jackson:$ktorVersion")

        implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
            exclude(group = "junit")
        }

        testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "junit")
        }
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

        testImplementation("com.opentable.components:otj-pg-embedded:0.13.4")
        testImplementation("io.mockk:mockk:1.12.0")
    }
}

subprojects {
    tasks {
        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
            }
        }
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "16"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "16"
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
                showStackTraces = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }

        }

        withType<Wrapper> {
            gradleVersion = "7.1.1"
        }
    }
}

tasks {
    named<Jar>("jar") { enabled = false }
}

gradle.buildFinished {
    project.buildDir.deleteRecursively()
}
