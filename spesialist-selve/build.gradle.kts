val graphQLKotlinVersion = "7.0.2"
val testcontainersVersion = "1.19.5"

plugins {
    kotlin("plugin.serialization") version "1.9.22"
    id("com.expediagroup.graphql") version "7.0.2"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    implementation(project(":spesialist-modell"))

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks {
    named<Test>("test") {
        systemProperty("junit.jupiter.execution.parallel.enabled", "false")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "2")
    }

    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }
    }

    val copyDeps by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        exclude("spesialist-*")
        into("build/deps")
    }
    val copyLibs by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        include("spesialist-*")
        into("build/libs")
    }
    named("assemble") {
        dependsOn(copyDeps, copyLibs)
    }
}
