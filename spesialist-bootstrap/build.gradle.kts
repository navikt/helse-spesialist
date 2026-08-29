plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.spesialist.bootstrap.RapidAppKt"
}

dependencies {
    api(project(":spesialist-api"))
    api(project(":clients:spesialist-client-entra-id"))
    api(project(":clients:spesialist-client-krr"))
    api(project(":clients:spesialist-client-sparkel-norg"))
    api(project(":clients:spesialist-client-sparkel-sykepengeperioder"))
    api(project(":clients:spesialist-client-speed"))
    api(project(":clients:spesialist-client-sp-forsikring"))
    api(project(":clients:spesialist-client-spleis"))
    api(project(":clients:spesialist-client-personpseudoid"))
    api(project(":clients:spesialist-client-tilgangsmaskinen"))
    api(project(":spesialist-db"))
    api(project(":spesialist-kafka"))
    api(project(":spesialist-valkey"))

    implementation(libs.rapidsAndRivers)
}

tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}
