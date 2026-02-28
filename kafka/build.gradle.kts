plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":application"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)
    implementation(libs.caffeine)

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation("io.github.optimumcode:json-schema-validator:0.5.3")
    testImplementation(testFixtures(project(":application")))
    testImplementation(testFixtures(project(":kafka")))
    testImplementation(testFixtures(project(":domain")))

    testFixturesImplementation(testFixtures(project(":domain")))
    testFixturesImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testFixturesImplementation(libs.rapidsAndRivers)
    testFixturesImplementation(libs.jackson.kotlin)
    testFixturesImplementation(libs.testcontainers.kafka)
}
