val junitJupiterVersion = "5.10.2"
val junitPlatformLauncherVersion = "1.10.2"
val ktorVersion = "2.3.8"
val jvmTargetVersion = "21"
val graphqlKotlinVersion = "7.0.2"
val rapidsAndRiversVersion = "2024020611421707216156.1a4e8bdae578"
val logbackSyslog4jVersion = "1.0.0"
val hikariCPVersion = "5.1.0"
val flywayCoreVersion = "10.7.2"
val kotliqueryVersion = "1.9.0"
val mockkVersion = "1.13.9"
val postgresqlVersion = "42.7.1"

plugins {
    kotlin("jvm") version "1.9.22"
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

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("io.ktor:ktor-server-websockets:$ktorVersion")
        implementation("org.postgresql:postgresql:$postgresqlVersion")
        implementation("com.papertrailapp:logback-syslog4j:$logbackSyslog4jVersion") //August, 2014
        {
            exclude(group = "ch.qos.logback")
        }
        implementation("com.zaxxer:HikariCP:$hikariCPVersion")
        implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
        implementation("org.flywaydb:flyway-database-postgresql:$flywayCoreVersion")
        implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        constraints {
            implementation("commons-codec:commons-codec") {
                version { require("1.13") }
                because("ktor-client-apache v2.3.7 -> httpclient -> commons-codec v1.11 har en sårbarhet")
            }
        }
        implementation("io.ktor:ktor-client-jackson:$ktorVersion") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
        }
        implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
        implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
        implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

        implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion") {
            exclude(group = "junit")
        }

        implementation("com.expediagroup:graphql-kotlin-client:$graphqlKotlinVersion")
        implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinVersion") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
        }
        implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlKotlinVersion") {
            exclude("com.expediagroup:graphql-kotlin-client-serialization")
        }
        implementation("io.ktor:ktor-server-cors:$ktorVersion")
        implementation("io.ktor:ktor-server-call-id:$ktorVersion")

        testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitPlatformLauncherVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
        testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "junit")
        }
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

tasks.jar {
    enabled = false
}
