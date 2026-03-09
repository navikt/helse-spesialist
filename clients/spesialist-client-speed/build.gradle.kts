plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.bundles.apache.httpclient5)
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.wiremock)
}
