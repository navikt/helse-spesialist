dependencies {
    api(project(":spesialist-db-migrations"))
    api(project(":spesialist-application"))

    implementation(libs.kotliquery)
    implementation(libs.bundles.flyway.postgres)
    implementation(libs.postgresJdbcDriver)
    implementation(libs.hikari)

    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testImplementation(libs.testcontainers.postgres)
}
