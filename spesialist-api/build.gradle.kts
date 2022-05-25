val testcontainersVersion = "1.17.1"

dependencies {
    implementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}
