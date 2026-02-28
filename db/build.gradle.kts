plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":db-migrations"))
    api(project(":application"))

    implementation(libs.kotliquery)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)

    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testFixturesImplementation(libs.testcontainers.postgres)
    testFixturesImplementation(libs.kotliquery)

    testImplementation(testFixtures(project(":application")))
    testImplementation(testFixtures(project(":domain")))
}
