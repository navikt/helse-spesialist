val junitJupiterVersion = "5.10.1"
val ktorVersion = "2.3.5"
val jvmTargetVersion = "17"
val graphqlKotlinVersion = "7.0.2"
val rapidsAndRiversVersion = "2023093008351696055717.ffdec6aede3d"
val logbackSyslog4jVersion = "1.0.0"
val hikariCPVersion = "5.0.1"
val flywayCoreVersion = "9.22.3"
val kotliqueryVersion = "1.9.0"
val mockkVersion = "1.13.8"
val postgresqlVersion = "42.6.0"

plugins {
    kotlin("jvm") version "1.9.10"
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
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
        implementation("io.ktor:ktor-server-cio:$ktorVersion")
        implementation("org.postgresql:postgresql:$postgresqlVersion")
        implementation("com.papertrailapp:logback-syslog4j:$logbackSyslog4jVersion") //August, 2014
        {
            exclude(group = "ch.qos.logback")
        }
        implementation("com.zaxxer:HikariCP:$hikariCPVersion")
        implementation("org.flywaydb:flyway-core:$flywayCoreVersion")
        implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-apache:$ktorVersion") {
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
    kotlin {
        jvmToolchain(17)
    }
    tasks {
        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
                showStackTraces = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
        withType<Wrapper>() {
            gradleVersion = "8.1"
        }
    }
}

tasks.jar {
    enabled = false
}
