val graphQLKotlinVersion = "7.0.0"
val testcontainersVersion = "1.19.0"

plugins {
    kotlin("plugin.serialization") version "1.9.0"
    id("com.expediagroup.graphql") version "7.0.0"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.35")
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
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

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/deps/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
