plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)
    implementation(libs.caffeine)

    testImplementation(libs.rapidsAndRiversTest)
    testImplementation(libs.mockk)
    testImplementation("io.github.optimumcode:json-schema-validator:0.5.5")
    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(testFixtures(project(":spesialist-domain")))

    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
    testFixturesImplementation(libs.rapidsAndRiversTest)
    testFixturesImplementation(libs.rapidsAndRivers)
    testFixturesImplementation(libs.bundles.jackson)
    testFixturesImplementation(libs.testcontainers.kafka)
}
