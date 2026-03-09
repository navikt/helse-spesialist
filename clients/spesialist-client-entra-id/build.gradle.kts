plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.bundles.apache.httpclient5)
    implementation(libs.caffeine)
    implementation(libs.bundles.jackson)

    // TODO: Trenger vi kanskje bare én av disse?
    implementation(libs.nimbus.joseJwt)
    implementation(libs.auth0.jwt)

    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.mockOauth2Server)
    testFixturesImplementation(libs.wiremock)
}
