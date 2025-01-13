dependencies {
    testImplementation(project(":spesialist-bootstrap"))
    testImplementation(libs.rapids.and.rivers)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.testcontainers.postgresql)
}
