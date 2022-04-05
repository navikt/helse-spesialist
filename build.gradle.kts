val junitJupiterVersion = "5.8.2"
val ktorVersion = "1.6.7"
val graphqlKotlinVersion = "5.3.2"
val jvmTargetVersion = "17"
val rapidsAndRiversVersion = "2022.04.05-09.40.11a466d7ac70"
val logbackSyslog4jVersion = "1.0.0"
val hikariCPVersion = "5.0.1"
val flywayCoreVersion = "8.5.2"
val kotliqueryVersion = "1.6.3"
val kotlinTestVersion = "1.5.21"
val mockkVersion = "1.12.3"
val postgressqlVersion = "42.3.3"

plugins {
    kotlin("jvm") version "1.6.10"
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
        implementation("com.zaxxer:HikariCP:$hikariCPVersion")
        implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
        implementation("com.github.seratch:kotliquery:$kotliqueryVersion") //April, 2019
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        implementation("io.ktor:ktor-client-jackson:$ktorVersion") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
        }
        implementation("io.ktor:ktor-jackson:$ktorVersion") {
            exclude(group = "com.fasterxml.jackson.core")
            exclude(group = "com.fasterxml.jackson.module")
        }

        implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
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

        testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinTestVersion")
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
