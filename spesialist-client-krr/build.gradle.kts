plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.wiremock)
}
