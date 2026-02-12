plugins {
    id("application")
}

application {
    mainClass.set("no.nav.helse.opprydding.AppKt")
    applicationName = "app"
}

dependencies {
    implementation(libs.postgresSocketFactory)
    implementation(libs.rapidsAndRivers)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.kotliquery)
    implementation(libs.hikari)
    implementation(libs.bundles.logback)

    testImplementation(project(":spesialist-db-migrations"))

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)

    testImplementation(testFixtures(project(":spesialist-db")))
}
