val testcontainersVersion = "1.19.7"
val cloudSqlVersion = "1.16.0"
val postgresqlVersion = "42.7.3"

dependencies {
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation(project(":spesialist-felles"))

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks {
    val copyDeps by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        into("build/libs")
    }
    named<Jar>("jar") {
        dependsOn(copyDeps)
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.migrering.AppKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
