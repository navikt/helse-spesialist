val graphQLKotlinVersion = "7.0.2"
val testcontainersVersion = "1.19.7"
val mockOAuth2ServerVersion = "2.1.2"

plugins {
    kotlin("plugin.serialization") version "2.0.0"
    id("com.expediagroup.graphql") version "7.1.4"
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
