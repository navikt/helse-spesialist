val graphQLKotlinVersion = "7.0.2"
val testcontainersVersion = "1.19.1"

plugins {
    kotlin("plugin.serialization") version "1.9.21"
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
        into("build/deps")
    }
    named("assemble") {
        dependsOn(copyDeps)
    }
}
