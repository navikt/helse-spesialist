plugins {
    id("no.nav.helse.sas.sas-kotlin")
}

dependencies {
    api(project(":spesialist-application"))
    implementation(libs.valkey.java)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    implementation(libs.micrometer.prometheus)
}
