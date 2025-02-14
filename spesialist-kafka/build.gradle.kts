dependencies {
    api(project(":spesialist-selve"))

    implementation(libs.rapidsAndRivers)
    implementation(libs.bundles.jackson)

    testImplementation(libs.tbdLibs.rapidsAndRiversTest)
}
