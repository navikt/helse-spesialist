dependencies {
    testImplementation(project(":bootstrap"))

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

    testImplementation(testFixtures(project(":application")))
    testImplementation(testFixtures(project(":db")))
    testImplementation(testFixtures(project(":api")))
    testImplementation(testFixtures(project(":kafka")))
    testImplementation(testFixtures(project(":domain")))
    testImplementation(testFixtures(project(":clients:entra-id")))
    testImplementation(testFixtures(project(":clients:krr")))
    testImplementation(testFixtures(project(":clients:spiskammerset")))
    testImplementation(testFixtures(project(":clients:spleis")))
    testImplementation(testFixtures(project(":clients:speed")))
    testImplementation(testFixtures(project(":clients:spillkar")))
}
