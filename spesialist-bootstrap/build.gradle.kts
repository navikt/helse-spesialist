plugins {
    kotlin("jvm")
}

group = "no.nav.helse"

val testcontainersVersion = "1.20.4"
val mockOAuth2ServerVersion = "2.1.10"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":spesialist-api"))
    implementation(project(":spesialist-selve"))
    implementation(project(":spesialist-modell"))
    implementation(project(":spesialist-db"))
    implementation(project(":spesialist-kafka"))

    implementation(libs.rapids.and.rivers)

    implementation(libs.hikari)
    implementation(libs.flyway.core)

    testImplementation(project(":spesialist-api-schema"))

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
}

kotlin {
    jvmToolchain(21)
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
            attributes["Main-Class"] = "no.nav.helse.RapidAppKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
