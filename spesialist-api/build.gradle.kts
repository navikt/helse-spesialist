plugins {
    id("com.expediagroup.graphql") version libs.versions.graphql.kotlin
}

dependencies {
    api(project(":spesialist-api-schema"))
    api(project(":spesialist-selve"))

    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation(libs.graphql.kotlin.ktor.server)

    implementation(libs.bundles.logging)
    implementation(libs.jackson.datatype)
    implementation(libs.jackson.helpers)

    implementation(libs.ktor.micrometer)
    implementation(libs.micrometer.prometheus)

    api(libs.bundles.ktor.server)
    api(libs.bundles.ktor.client)

    testImplementation(libs.bundles.ktor.server.test)
}
