dependencies {
    api(project(":spesialist-selve"))

    implementation(libs.bundles.ktor.client)
    implementation(libs.jackson.datatype)

    // TODO: Trenger vi kanskje bare én av disse?
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("com.auth0:java-jwt:4.4.0")
}
