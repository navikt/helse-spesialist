plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":application"))

    implementation(libs.apache.httpclient5.fluent)
    implementation(libs.caffeine)
    implementation(libs.bundles.jackson)

    // TODO: Trenger vi kanskje bare én av disse?
    implementation(libs.nimbus.joseJwt)
    implementation(libs.auth0.jwt)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.mockOauth2Server)
    testFixturesImplementation(libs.wiremock)
}
