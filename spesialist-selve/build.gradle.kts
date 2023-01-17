val graphQLKotlinVersion = "6.3.4"
val testcontainersVersion = "1.17.3"

plugins {
    kotlin("plugin.serialization") version "1.6.21"
    id("com.expediagroup.graphql") version "5.4.1"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.25.6")
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
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
