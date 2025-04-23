plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation("io.github.optimumcode:json-schema-validator:0.5.1")
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(testFixtures(project(":spesialist-domain")))

    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
    testFixturesImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testFixturesImplementation(libs.rapidsAndRivers)
    testFixturesImplementation(libs.jackson.kotlin)
    testFixturesImplementation(libs.testcontainers.kafka)
}
