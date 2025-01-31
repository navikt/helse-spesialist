val graphQLKotlinVersion = "8.2.1"

plugins {
    id("com.expediagroup.graphql") version "8.3.0"
}

dependencies {
    api(project(":spesialist-selve"))

    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    api("com.expediagroup:graphql-kotlin-ktor-server:$graphQLKotlinVersion")

    implementation(libs.bundles.logging)
    implementation(libs.jackson.datatype)
    implementation(libs.jackson.helpers)

    implementation(libs.ktor.micrometer)
    implementation(libs.micrometer.prometheus)

    api(libs.bundles.ktor.server)
    api(libs.bundles.ktor.client)

    testImplementation(libs.bundles.ktor.server.test)
}
