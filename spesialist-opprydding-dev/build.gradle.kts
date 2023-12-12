val testcontainersVersion = "1.19.1"
val cloudSqlVersion = "1.15.0"
val postgresqlVersion = "42.6.0"

val mainClass = "no.nav.helse.opprydding.AppKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    testImplementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion") {
        exclude("com.fasterxml.jackson.core")
    }
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
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
