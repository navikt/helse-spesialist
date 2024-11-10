plugins {
    kotlin("jvm")
}

group = "no.nav.helse"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":spesialist-selve"))
    implementation(project(":spesialist-api"))
    implementation(project(":spesialist-felles"))
    implementation(libs.rapids.and.rivers)
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
