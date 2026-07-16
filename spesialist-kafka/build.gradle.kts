plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)
    implementation(libs.caffeine)

    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
    testFixturesImplementation(libs.rapidsAndRiversTest)
    testFixturesImplementation(libs.testcontainers.kafka)

    testImplementation(libs.mockk)
    testImplementation("io.github.optimumcode:json-schema-validator:0.5.5")
    testImplementation(testFixtures(project(":spesialist-application")))
}
