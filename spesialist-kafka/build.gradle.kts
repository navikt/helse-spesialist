dependencies {
    api(project(":spesialist-application"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
}
