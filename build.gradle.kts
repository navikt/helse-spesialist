val junitJupiterVersion = "5.9.0"
val ktorVersion = "2.1.1"
val graphqlKotlinVersion = "6.2.5"
val jvmTargetVersion = "17"
val rapidsAndRiversVersion = "2022092314391663936769.9d5d33074875"
val logbackSyslog4jVersion = "1.0.0"
val hikariCPVersion = "5.0.1"
val flywayCoreVersion = "9.8.1"
val kotliqueryVersion = "1.9.0"
val kotlinTestVersion = "1.6.21"
val mockkVersion = "1.12.8"
val postgressqlVersion = "42.5.0"

plugins {
    kotlin("jvm") version "1.6.21"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("org.postgresql:postgresql:$postgressqlVersion")
        implementation("com.papertrailapp:logback-syslog4j:$logbackSyslog4jVersion") //August, 2014
        {
            exclude(group = "ch.qos.logback")
        }
        implementation("com.zaxxer:HikariCP:$hikariCPVersion")
        implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
        implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        {
            exclude(group = "commons-codec")
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


        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "junit")
        }
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

        testImplementation("io.mockk:mockk:$mockkVersion")
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
            kotlinOptions.jvmTarget = jvmTargetVersion
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = jvmTargetVersion
        }

        withType<Wrapper> {
            gradleVersion = "7.4.2"
        }

        withType<Test> {
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
    named<Jar>("jar") { enabled = false }
}

tasks {
    named("build") {
        finalizedBy()
        project.buildDir.deleteRecursively()
    }
}
