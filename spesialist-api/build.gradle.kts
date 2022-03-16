val testcontainersVersion = "1.16.2"

dependencies {
    implementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}
