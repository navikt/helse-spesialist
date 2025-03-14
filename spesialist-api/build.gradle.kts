plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-api-schema"))
    api(project(":spesialist-application"))

    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.bundles.ktor.server)
    implementation(libs.nimbus.joseJwt)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    implementation(libs.micrometer.prometheus)

    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(libs.bundles.ktor.server.test)

    testFixturesImplementation(libs.graphqlKotlin.server.ktor)
}
