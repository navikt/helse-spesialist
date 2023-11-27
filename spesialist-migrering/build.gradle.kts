val testcontainersVersion = "1.19.1"
val cloudSqlVersion = "1.14.1"
val postgresqlVersion = "42.6.0"

dependencies {
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
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
    }
    val copyDeps by registering(Sync::class) {
        from(configurations.runtimeClasspath)
        into("build/libs")
    }
    named("assemble") {
        dependsOn(copyDeps)
    }
}
