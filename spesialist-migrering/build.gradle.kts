val testcontainersVersion = "1.17.3"

dependencies {
    implementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.migrering.AppKt"
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