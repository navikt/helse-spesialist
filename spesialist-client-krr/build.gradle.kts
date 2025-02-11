dependencies {
    api(project(":spesialist-selve"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.jackson.datatype)
    implementation(libs.micrometer.prometheus)
}
