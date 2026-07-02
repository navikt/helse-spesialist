plugins {
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))
    implementation(libs.tbdLibs.populasjonstilgangskontroll.tilgangsmaskinen)

    testFixturesImplementation(libs.wiremock)
}
