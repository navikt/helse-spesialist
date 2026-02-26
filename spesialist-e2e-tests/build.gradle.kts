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
    testImplementation(libs.apache.httpclient5.fluent)

    testImplementation(libs.bundles.jackson)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.wiremock)

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-db")))
    testImplementation(testFixtures(project(":spesialist-api")))
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(testFixtures(project(":clients:spesialist-client-entra-id")))
    testImplementation(testFixtures(project(":clients:spesialist-client-krr")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spiskammerset")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spleis")))
    testImplementation(testFixtures(project(":clients:spesialist-client-speed")))
    testImplementation(testFixtures(project(":clients:spesialist-client-spillkar")))
}
