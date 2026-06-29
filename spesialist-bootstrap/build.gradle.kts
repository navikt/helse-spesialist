dependencies {
    api(project(":spesialist-api"))
    api(project(":clients:spesialist-client-entra-id"))
    api(project(":clients:spesialist-client-krr"))
    api(project(":clients:spesialist-client-sparkel-norg"))
    api(project(":clients:spesialist-client-sparkel-sykepengeperioder"))
    api(project(":clients:spesialist-client-speed"))
    api(project(":clients:spesialist-client-spillkar"))
    api(project(":clients:spesialist-client-spiskammerset"))
    api(project(":clients:spesialist-client-spleis"))
    api(project(":clients:spesialist-client-personpseudoid"))
    api(project(":spesialist-db"))
    api(project(":spesialist-kafka"))
    api(project(":spesialist-valkey"))

    implementation(libs.rapidsAndRivers)

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-db")))
    testImplementation(testFixtures(project(":spesialist-api")))
    testImplementation(testFixtures(project(":clients:spesialist-client-entra-id")))
    testImplementation(testFixtures(project(":clients:spesialist-client-krr")))
    testImplementation(testFixtures(project(":clients:spesialist-client-sparkel-norg")))
    testImplementation(testFixtures(project(":clients:spesialist-client-sparkel-sykepengeperioder")))
    testImplementation(testFixtures(project(":clients:spesialist-client-speed")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spillkar")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spiskammerset")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spleis")))
    testImplementation(testFixtures(project(":clients:spesialist-client-personpseudoid")))
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(libs.mockOauth2Server)
}

tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks {
    val copyDeps =
        register<Sync>("copyDeps") {
            description = "Kopierer runtime-avhengigheter til deps-mappa"
            from(configurations.runtimeClasspath)
            exclude("spesialist-*")
            into(layout.buildDirectory.dir("deps"))
        }
    val copyLibs =
        register<Sync>("copyLibs") {
            description = "Kopierer appens egne jar-filer til libs-mappa"
            from(configurations.runtimeClasspath)
            include("spesialist-*")
            into(layout.buildDirectory.dir("libs"))
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
