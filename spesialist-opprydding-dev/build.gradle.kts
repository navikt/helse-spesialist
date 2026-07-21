plugins {
    id("no.nav.helse.sas.sas-deployable")
}

sasDeployable {
    mainClass = "no.nav.helse.opprydding.AppKt"
    imageName = "${rootProject.name}-opprydding-dev"
}

dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.kotliquery)
    implementation(libs.hikari)
    implementation(libs.bundles.logback)

    testImplementation(project(":spesialist-db-migrations"))

    testImplementation(libs.rapidsAndRiversTest)

    testImplementation(testFixtures(project(":spesialist-db")))
}
