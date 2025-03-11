val mainClass = "no.nav.helse.sidegig.AppKt"

dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.rapidsAndRivers)
    implementation(libs.hikari)
    implementation(libs.kotliquery)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.bundles.logback)

    testImplementation(project(":spesialist-db-migrations"))

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)

    testImplementation(testFixtures(project(":spesialist-db")))
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
