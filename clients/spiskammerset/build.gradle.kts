plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":application"))

    implementation(libs.apache.httpclient5.fluent)
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.wiremock)
}
