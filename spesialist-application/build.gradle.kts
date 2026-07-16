plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-domain"))

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    // Nødvendig for audit logging, men dette biblioteket har ikke blitt oppdatert siden august 2014 (!)
    implementation(libs.logback.syslog4j)
    api(libs.bundles.kotlinx.coroutines)
    api(libs.tbdLibs.accessTokenProviderApi)
    api(libs.tbdLibs.populasjonstilgangskontroll.api)
    implementation(libs.micrometer.prometheus)
    implementation(libs.opentelemetry.instrumentation.annotations)

    testImplementation(testFixtures(project(":spesialist-domain")))
    testImplementation(libs.mockk)

    testFixturesImplementation(testFixtures(project(":spesialist-domain")))
    testFixturesImplementation(kotlin("test"))
    testFixturesImplementation(libs.bundles.jackson)
    testFixturesImplementation(libs.bundles.logback)
}
