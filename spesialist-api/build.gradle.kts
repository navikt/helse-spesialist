plugins {
    id("com.expediagroup.graphql") version libs.versions.graphql.kotlin
}

dependencies {
    implementation(project(":spesialist-api-schema"))
    implementation(project(":spesialist-selve"))
    implementation(project(":spesialist-modell"))

    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation(libs.graphql.kotlin.ktor.server)

    implementation(libs.bundles.logging)
    implementation(libs.jackson.datatype)
    implementation(libs.jackson.helpers)

    implementation(libs.ktor.micrometer)
    implementation(libs.micrometer.prometheus)

    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)

    testImplementation(libs.bundles.ktor.server.test)
}
