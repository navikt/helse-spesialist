val junitJupiterVersion = "5.8.2"
val ktorVersion = "1.6.7"
val graphqlKotlinVersion = "5.3.1"
val jvmTargetVersion = "17"

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
        implementation("com.github.navikt:rapids-and-rivers:2022.01.19-09.53.b526ca84a9e4")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("com.papertrailapp:logback-syslog4j:1.0.0") //August, 2014
        implementation("com.zaxxer:HikariCP:5.0.1")
        implementation("no.nav:vault-jdbc:1.3.7")
        implementation("org.flywaydb:flyway-core:8.4.1")
        implementation("com.github.seratch:kotliquery:1.6.0") //April, 2019
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion")
        implementation("io.ktor:ktor-client-jackson:$ktorVersion")
        implementation("io.ktor:ktor-jackson:$ktorVersion")

        implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
            exclude(group = "junit")
        }

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

        withType<Wrapper> {
            gradleVersion = "7.3.3"
        }
    }
}

tasks {
    named<Jar>("jar") { enabled = false }
}

gradle.buildFinished {
    project.buildDir.deleteRecursively()
}
