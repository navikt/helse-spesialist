plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":application"))

    implementation(libs.graphqlKotlin.client.jackson)
    implementation(libs.apache.httpclient5.fluent)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)

    testFixturesImplementation(libs.wiremock)
}
