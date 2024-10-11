val graphQLKotlinVersion = "8.1.0"
val testcontainersVersion = "1.20.2"
val mockOAuth2ServerVersion = "2.1.9"

plugins {
    kotlin("plugin.serialization") version "2.0.20"
    id("com.expediagroup.graphql") version "8.1.0"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    implementation(project(":spesialist-modell"))

    testImplementation(testFixtures(project(":spesialist-felles")))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
}

tasks {
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

    named<Jar>("jar") {
        dependsOn(copyDeps, copyLibs)
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.MainKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
