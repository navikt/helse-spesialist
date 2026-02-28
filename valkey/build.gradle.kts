dependencies {
    api(project(":application"))
    implementation(libs.valkey.java)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logback)
}
