plugins {
    id("no.nav.sykepenger.kotlin")
    `java-test-fixtures`
}

dependencies {
    api(project(":spesialist-application"))

    implementation(libs.tbdLibs.personspeudoid)

    testFixturesImplementation(libs.testcontainers.core)
}
