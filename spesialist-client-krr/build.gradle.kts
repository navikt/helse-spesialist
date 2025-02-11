dependencies {
    api(project(":spesialist-selve"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.jackson.datatype)
    implementation(libs.micrometer.prometheus)

    testImplementation("org.wiremock:wiremock:3.11.0")
}
