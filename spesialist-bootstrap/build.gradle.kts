plugins {
    kotlin("jvm")
}

group = "no.nav.helse"

val testcontainersVersion = "1.20.4"
val mockOAuth2ServerVersion = "2.1.9"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":spesialist-kafka"))
    api(project(":spesialist-selve"))
    api(project(":spesialist-api"))
    api(project(":spesialist-felles"))
    implementation(libs.rapids.and.rivers)

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    api("org.testcontainers:kafka:$testcontainersVersion")
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
