plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.jackson)

    // TODO: Trenger vi kanskje bare én av disse?
    implementation(libs.nimbus.joseJwt)
    implementation(libs.auth0.jwt)

    testFixturesImplementation(libs.mockOauth2Server)
    testFixturesImplementation(libs.wiremock)
}
