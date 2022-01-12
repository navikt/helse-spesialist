val junitJupiterVersion = "5.8.1"
val ktorVersion = "1.6.6"
val graphqlKotlinVersion = "5.3.1"

plugins {
    kotlin("jvm") version "1.6.0"
}

allprojects {
    group = "no.nav.helse"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:2021.11.29-21.57.443e21ff5a6c")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("com.papertrailapp:logback-syslog4j:1.0.0") //August, 2014
        implementation("com.zaxxer:HikariCP:5.0.0")
        implementation("no.nav:vault-jdbc:1.3.7")
        implementation("org.flywaydb:flyway-core:8.1.0")
        implementation("com.github.seratch:kotliquery:1.6.0") //April, 2019
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        implementation("io.ktor:ktor-client-jackson:$ktorVersion")
        implementation("io.ktor:ktor-jackson:$ktorVersion")

        implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
            exclude(group = "junit")
        }

        implementation("no.finn.unleash:unleash-client-java:4.4.0")

        implementation("com.expediagroup:graphql-kotlin-client:$graphqlKotlinVersion")
        implementation("com.expediagroup:graphql-kotlin-client-jackson:$graphqlKotlinVersion")
        implementation("com.expediagroup:graphql-kotlin-ktor-client:$graphqlKotlinVersion") {
            exclude("com.expediagroup:graphql-kotlin-client-serialization")
        }

        testImplementation("org.jetbrains.kotlin:kotlin-test:1.5.21")
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
            exclude(group = "junit")
        }
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

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
            gradleVersion = "7.3"
        }
    }
}

tasks {
    named<Jar>("jar") { enabled = false }
}

gradle.buildFinished {
    project.buildDir.deleteRecursively()
}
