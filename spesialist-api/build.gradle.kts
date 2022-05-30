val testcontainersVersion = "1.17.2"

dependencies {
    implementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}
