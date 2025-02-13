dependencies {
    api(project(":spesialist-modell"))

    implementation(libs.bundles.logging)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.micrometer.prometheus)
}
