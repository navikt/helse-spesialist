plugins {
    id("no.nav.helse.sas.sas-kotlin")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.bundles.logback)

    testImplementation(libs.mockk)
}
