val testcontainersVersion = "1.17.6"
val cloudSqlVersion = "1.11.0"
val postgresqlVersion = "42.5.4"

dependencies {
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks.named<Jar>("jar") {
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