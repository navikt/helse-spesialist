dependencies {
    api(project(":spesialist-selve"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.jackson)

    // TODO: Trenger vi kanskje bare én av disse?
    implementation(libs.nimbus.joseJwt)
    implementation(libs.auth0.jwt)
}
