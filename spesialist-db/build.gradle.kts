plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-db-migrations"))
    api(project(":spesialist-application"))

    implementation(libs.kotliquery)
    implementation(libs.hikari)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.postgresPgJdbcDriver)

    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testFixturesImplementation(libs.testcontainers.postgres)
    testFixturesImplementation(libs.kotliquery)

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-domain")))
}
