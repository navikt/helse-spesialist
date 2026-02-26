dependencies {
    api(project(":spesialist-api"))
    api(project(":clients:spesialist-client-entra-id"))
    api(project(":clients:spesialist-client-krr"))
    api(project(":clients:spesialist-client-speed"))
    api(project(":clients:spesialist-client-spillkar"))
    api(project(":clients:spesialist-client-spiskammerset"))
    api(project(":clients:spesialist-client-spleis"))
    api(project(":spesialist-db"))
    api(project(":spesialist-kafka"))
    api(project(":spesialist-valkey"))

    implementation(libs.rapidsAndRivers)

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-db")))
    testImplementation(testFixtures(project(":spesialist-api")))
    testImplementation(testFixtures(project(":clients:spesialist-client-entra-id")))
    testImplementation(testFixtures(project(":clients:spesialist-client-krr")))
    testImplementation(testFixtures(project(":clients:spesialist-client-speed")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spillkar")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spiskammerset")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spleis")))
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(libs.mockOauth2Server)
}

tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
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
            attributes["Main-Class"] = "no.nav.helse.spesialist.bootstrap.RapidAppKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
