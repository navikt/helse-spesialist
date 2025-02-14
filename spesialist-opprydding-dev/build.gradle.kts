val mainClass = "no.nav.helse.opprydding.AppKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.cloudsql)
    implementation(libs.rapids.and.rivers)
    implementation(libs.postgres)
    implementation(libs.kotliquery)
    implementation(libs.hikari)
    implementation(libs.bundles.logback)

    testImplementation(project(":spesialist-db-migrations"))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.pg)
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
