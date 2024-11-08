val graphQLKotlinVersion = "8.1.0"
val testcontainersVersion = "1.20.2"
val mockOAuth2ServerVersion = "2.1.9"

plugins {
    kotlin("plugin.serialization") version "2.0.20"
    id("com.expediagroup.graphql") version "8.1.0"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation(libs.rapids.and.rivers)
    implementation("com.expediagroup:graphql-kotlin-ktor-server:$graphQLKotlinVersion")
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    implementation(project(":spesialist-modell"))

    testImplementation(testFixtures(project(":spesialist-felles")))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
}
