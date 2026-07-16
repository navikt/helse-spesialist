plugins {
    id("application")
}

application {
    mainClass.set("no.nav.helse.spesialist.bootstrap.RapidAppKt")
    applicationName = "app"
}

dependencies {
    api(project(":spesialist-api"))
    api(project(":clients:spesialist-client-entra-id"))
    api(project(":clients:spesialist-client-krr"))
    api(project(":clients:spesialist-client-sparkel-norg"))
    api(project(":clients:spesialist-client-sparkel-sykepengeperioder"))
    api(project(":clients:spesialist-client-speed"))
    api(project(":clients:spesialist-client-spillkar"))
    api(project(":clients:spesialist-client-sp-forsikring"))
    api(project(":clients:spesialist-client-spleis"))
    api(project(":clients:spesialist-client-personpseudoid"))
    api(project(":clients:spesialist-client-tilgangsmaskinen"))
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
    testImplementation(testFixtures(project(":clients:spesialist-client-sp-forsikring")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spleis")))
    testImplementation(testFixtures(project(":clients:spesialist-client-personpseudoid")))
    testImplementation(testFixtures(project(":clients:spesialist-client-tilgangsmaskinen")))
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(libs.mockOauth2Server)
}

tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}
