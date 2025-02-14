dependencies {
    testImplementation(project(":spesialist-bootstrap"))

    testImplementation(libs.rapidsAndRivers)
    testImplementation(libs.tbdLibs.rapidsAndRiversTest)

    testImplementation(libs.bundles.flyway.postgres)
    testImplementation(libs.hikari)
    testImplementation(libs.kotliquery)
    testImplementation(libs.testcontainers.postgres)

    testImplementation(libs.bundles.ktor.server)
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.graphqlKotlin.server.ktor)

    testImplementation(libs.bundles.ktor.client)

    testImplementation(libs.bundles.jackson)
}
