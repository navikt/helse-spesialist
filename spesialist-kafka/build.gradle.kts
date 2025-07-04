plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.1")

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testImplementation("io.github.optimumcode:json-schema-validator:0.5.2")
    testImplementation(testFixtures(project(":spesialist-kafka")))
    testImplementation(testFixtures(project(":spesialist-domain")))

    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
    testFixturesImplementation(libs.tbdLibs.rapidsAndRiversTest)
    testFixturesImplementation(libs.rapidsAndRivers)
    testFixturesImplementation(libs.jackson.kotlin)
    testFixturesImplementation(libs.testcontainers.kafka)
}
