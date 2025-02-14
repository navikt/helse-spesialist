dependencies {
    api(project(":spesialist-api-schema"))
    api(project(":spesialist-selve"))

    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation(libs.graphql.kotlin.ktor.server)

    implementation(libs.bundles.logback)
    implementation(libs.jackson.jsr310)
    implementation(libs.tbs.libs.jackson)
    implementation(libs.jackson.kotlin)

    implementation(libs.ktor.micrometer)
    implementation(libs.micrometer.prometheus)

    api(libs.bundles.ktor.server)
    api(libs.bundles.ktor.client)

    testImplementation(libs.bundles.ktor.server.test)
}
