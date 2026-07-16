plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-api-schema"))
    api(project(":spesialist-application"))

    implementation(libs.graphqlKotlin.server.ktor)
    implementation(libs.bundles.ktor.server)
    implementation(libs.nimbus.joseJwt)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    implementation(libs.micrometer.prometheus)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.smiley4.ktor.openapi.tools)
    implementation(libs.opentelemetry.instrumentation.annotations)

    constraints {
        // CVE-2025-66453: Number.toFixed() DoS in Rhino, fixed in 1.7.14.1 / 1.7.15.1 / 1.8.1.
        // Pulled in transitively via ktor-swagger-ui -> swagger-compat-spec-parser -> json-schema-validator.
        implementation("org.mozilla:rhino:1.9.1") {
            because("CVE-2025-66453: forces the patched Rhino version over the vulnerable transitive 1.7.7.2")
        }
    }

    testFixturesImplementation(libs.mockOauth2Server)
    testFixturesImplementation(libs.tbdLibs.populasjonstilgangskontroll.api)
    testFixturesImplementation(testFixtures(project(":spesialist-domain")))

    testImplementation(testFixtures(project(":spesialist-application")))
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.mockk)
}
