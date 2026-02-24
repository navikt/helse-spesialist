plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-db-migrations"))
    api(project(":spesialist-application"))

    implementation(libs.kotliquery)
    implementation(libs.hikari)

    implementation("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testFixturesImplementation(libs.testcontainers.postgres)
    testFixturesImplementation(libs.kotliquery)

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(testFixtures(project(":spesialist-domain")))
}
