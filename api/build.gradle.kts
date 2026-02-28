plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":api-schema"))
    api(project(":application"))

    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.bundles.ktor.server)
    implementation(libs.nimbus.joseJwt)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    implementation(libs.micrometer.prometheus)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.smiley4.ktor.openapi.tools)

    testImplementation(testFixtures(project(":application")))
    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.bundles.ktor.client)

    testImplementation(libs.mockOauth2Server)

    testFixturesImplementation(libs.graphqlKotlin.server.ktor)
    testFixturesImplementation(libs.bundles.ktor.server)
    testFixturesImplementation(libs.mockOauth2Server)
    testFixturesImplementation(testFixtures(project(":domain")))
}
