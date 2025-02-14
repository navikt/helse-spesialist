dependencies {
    api(project(":spesialist-api-schema"))
    api(project(":spesialist-selve"))

    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.bundles.ktor.server)
    implementation(libs.nimbus.joseJwt)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    implementation(libs.micrometer.prometheus)

    testImplementation(libs.bundles.ktor.server.test)
}
