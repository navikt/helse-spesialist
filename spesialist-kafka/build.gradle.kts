dependencies {
    api(project(":spesialist-selve"))

    api(libs.rapids.and.rivers)
    implementation(libs.jackson.datatype)
    implementation(libs.jackson.helpers)
    implementation(libs.jackson.kotlin)

    testImplementation(libs.rapids.and.rivers.test)
}
