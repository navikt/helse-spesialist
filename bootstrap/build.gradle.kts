dependencies {
    api(project(":api"))
    api(project(":clients:entra-id"))
    api(project(":clients:krr"))
    api(project(":clients:speed"))
    api(project(":clients:spillkar"))
    api(project(":clients:spiskammerset"))
    api(project(":clients:spleis"))
    api(project(":db"))
    api(project(":kafka"))
    api(project(":valkey"))

    implementation(libs.rapidsAndRivers)

    testImplementation(testFixtures(project(":application")))
    testImplementation(testFixtures(project(":db")))
    testImplementation(testFixtures(project(":api")))
    testImplementation(testFixtures(project(":clients:entra-id")))
    testImplementation(testFixtures(project(":clients:krr")))
    testImplementation(testFixtures(project(":clients:speed")))
    testImplementation(testFixtures(project(":clients:spillkar")))
    testImplementation(testFixtures(project(":clients:spiskammerset")))
    testImplementation(testFixtures(project(":clients:spleis")))
    testImplementation(testFixtures(project(":kafka")))
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
