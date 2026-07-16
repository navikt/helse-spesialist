plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-domain"))
    api(libs.bundles.kotlinx.coroutines)
    api(libs.tbdLibs.accessTokenProviderApi)
    api(libs.tbdLibs.populasjonstilgangskontroll.api)

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    // Nødvendig for audit logging, men dette biblioteket har ikke blitt oppdatert siden august 2014 (!)
    implementation(libs.logback.syslog4j)
    implementation(libs.micrometer.prometheus)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
    testFixturesImplementation(kotlin("test"))

    testImplementation(libs.mockk)
}
