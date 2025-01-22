val graphQLKotlinVersion = "8.2.1"

val micrometerRegistryPrometheusVersion = "1.13.6"
val logbackEncoderVersion = "8.0"

plugins {
    kotlin("plugin.serialization") version "2.0.20"
    id("com.expediagroup.graphql") version "8.2.1"
}

dependencies {
    api(project(":spesialist-api"))

    api("com.nimbusds:nimbus-jose-jwt:9.37.3")
    api(libs.bundles.db)

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")

    testImplementation(libs.bundles.ktor.server)
    testImplementation("com.expediagroup:graphql-kotlin-ktor-server:$graphQLKotlinVersion")
    testImplementation(libs.testcontainers.postgresql)
}
