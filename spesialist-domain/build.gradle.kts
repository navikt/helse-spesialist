plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    implementation(libs.bundles.logback)

    testImplementation(libs.mockk)
}
