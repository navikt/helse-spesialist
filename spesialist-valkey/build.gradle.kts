dependencies {
    api(project(":spesialist-application"))
    implementation(libs.valkey.java)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
}
