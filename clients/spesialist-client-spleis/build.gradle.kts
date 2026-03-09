plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.graphqlKotlin.client.jackson)
    implementation(libs.bundles.apache.httpclient5)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)

    testFixturesImplementation(libs.wiremock)
}
