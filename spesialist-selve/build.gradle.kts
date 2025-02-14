dependencies {
    api(project(":spesialist-modell"))

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
    // Nødvendig for audit logging, men dette biblioteket har ikke blitt oppdatert siden august 2014 (!)
    implementation(libs.logback.syslog4j)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.micrometer.prometheus)
}
