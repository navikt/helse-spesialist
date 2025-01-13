val cloudSqlVersion = "1.16.0"
val postgresqlVersion = "42.7.3"

val mainClass = "no.nav.helse.opprydding.AppKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation(libs.rapids.and.rivers)

    testImplementation(project(":spesialist-felles"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.rapids.and.rivers.test)
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
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
