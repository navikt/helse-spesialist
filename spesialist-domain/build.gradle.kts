plugins {
    `java-test-fixtures`
}

dependencies {
    implementation(libs.bundles.logback)
    testImplementation(libs.mockk)
    testImplementation(testFixtures(project(":spesialist-domain")))
}
