dependencies {
    api(project(":spesialist-selve"))

    api(libs.rapids.and.rivers)
    implementation(libs.jackson.jsr310)
    implementation(libs.tbs.libs.jackson)
    implementation(libs.jackson.kotlin)

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
}
