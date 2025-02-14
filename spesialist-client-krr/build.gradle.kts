dependencies {
    api(project(":spesialist-selve"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testImplementation(libs.wiremock)
}
