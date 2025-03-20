plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.unleash.client)

    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.wiremock)
}
