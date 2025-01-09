val graphQLKotlinVersion = "8.2.1"
val testcontainersVersion = "1.20.4"
val mockOAuth2ServerVersion = "2.1.9"

plugins {
    kotlin("plugin.serialization") version "2.0.20"
    id("com.expediagroup.graphql") version "8.2.1"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation(libs.rapids.and.rivers)
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    implementation(project(":spesialist-modell"))

    testImplementation(testFixtures(project(":spesialist-felles")))
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.bundles.ktor.server)
    testImplementation("com.expediagroup:graphql-kotlin-ktor-server:$graphQLKotlinVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    api("org.testcontainers:kafka:$testcontainersVersion")
}
