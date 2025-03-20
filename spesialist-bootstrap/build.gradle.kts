dependencies {
    api(project(":spesialist-api"))
    api(project(":spesialist-client-entra-id"))
    api(project(":spesialist-client-krr"))
    api(project(":spesialist-client-spleis"))
    api(project(":spesialist-db"))
    api(project(":spesialist-kafka"))

    implementation(libs.unleash.client)
    implementation(libs.bundles.logback)
    implementation(libs.rapidsAndRivers)
    implementation(libs.micrometer.prometheus)

    testImplementation(libs.hikari)
    testImplementation(libs.bundles.flyway.postgres)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.wiremock)

    testImplementation(testFixtures(project(":spesialist-db")))
    testImplementation(testFixtures(project(":spesialist-api")))
    testImplementation(testFixtures(project(":spesialist-client-entra-id")))
    testImplementation(testFixtures(project(":spesialist-client-krr")))
    testImplementation(testFixtures(project(":spesialist-client-spleis")))
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
