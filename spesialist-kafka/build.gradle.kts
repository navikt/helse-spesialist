dependencies {
    implementation(project(":spesialist-selve"))
    implementation(project(":spesialist-modell"))

    implementation(libs.rapids.and.rivers)

    testImplementation(libs.rapids.and.rivers.test)
}
