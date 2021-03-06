val junitJupiterVersion = "5.7.1"
val ktorVersion = "1.5.1"

plugins {
    kotlin("jvm") version "1.4.30"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        jcenter()
        maven("https://jitpack.io")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:20210617121814-3e67e4d")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("com.papertrailapp:logback-syslog4j:1.0.0") //August, 2014
        implementation("com.zaxxer:HikariCP:4.0.2")
        implementation("no.nav:vault-jdbc:1.3.7")
        implementation("org.flywaydb:flyway-core:7.5.4")
        implementation("com.github.seratch:kotliquery:1.3.1") //April, 2019
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        implementation("io.ktor:ktor-client-jackson:$ktorVersion")
        implementation("io.ktor:ktor-jackson:$ktorVersion")

        implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
            exclude(group = "junit")
        }

        implementation("no.finn.unleash:unleash-client-java:4.1.0")

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "junit")
        }
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

        testImplementation("com.opentable.components:otj-pg-embedded:0.13.3") //Oktober, 2019
        testImplementation("io.mockk:mockk:1.10.6")
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
            kotlinOptions.jvmTarget = "15"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "15"
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
            }
        }

        withType<Wrapper> {
            gradleVersion = "6.8.3"
        }
    }
}

tasks {
    named<Jar>("jar") { enabled = false }
}

gradle.buildFinished {
    project.buildDir.deleteRecursively()
}
