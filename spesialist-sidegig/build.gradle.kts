val mainClass = "no.nav.helse.sidegig.AppKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.postgres.socket.factory)
    implementation(libs.rapids.and.rivers)
    implementation(libs.hikari)
    implementation(libs.kotliquery)
    implementation(libs.postgres)
    implementation(libs.bundles.logback)

    testImplementation(kotlin("test"))
    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgres)
    testImplementation(project(":spesialist-db-migrations"))
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
