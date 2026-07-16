plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-api-schema"))
    api(project(":spesialist-application"))

    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.bundles.ktor.server)
    implementation(libs.nimbus.joseJwt)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    implementation(libs.micrometer.prometheus)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.smiley4.ktor.openapi.tools)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.mockk)

    testImplementation(libs.mockOauth2Server)

    testFixturesImplementation(libs.graphqlKotlin.server.ktor)
    testFixturesImplementation(platform("io.ktor:ktor-bom:3.5.1"))
    testFixturesImplementation(libs.bundles.ktor.server)
    testFixturesImplementation(libs.mockOauth2Server)
    testFixturesImplementation(libs.tbdLibs.populasjonstilgangskontroll.api)
    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
}
