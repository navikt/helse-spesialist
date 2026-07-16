plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.tbdLibs.accessTokenProviderTexas)
    implementation(libs.apache.httpclient5.fluent)
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testFixturesImplementation(libs.wiremock)

    testImplementation(testFixtures(project(":spesialist-domain")))
}
