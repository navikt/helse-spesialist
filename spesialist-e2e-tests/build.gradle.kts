dependencies {
    testImplementation(project(":spesialist-bootstrap"))

    testImplementation(libs.rapidsAndRivers)
    testImplementation(libs.tbdLibs.rapidsAndRiversTest)

    testImplementation(libs.bundles.flyway.postgres)
    testImplementation(libs.hikari)
    testImplementation(libs.kotliquery)

    testImplementation(libs.bundles.ktor.server)
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.graphqlKotlin.server.ktor)

    testImplementation(libs.bundles.ktor.client)

    testImplementation(libs.bundles.jackson)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.wiremock)

    testImplementation(testFixtures(project(":spesialist-db")))
    testImplementation(testFixtures(project(":spesialist-api")))
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(testFixtures(project(":spesialist-client-entra-id")))
    testImplementation(testFixtures(project(":spesialist-client-krr")))
    testImplementation(testFixtures(project(":spesialist-client-spleis")))
    testImplementation(testFixtures(project(":spesialist-client-unleash")))
}
