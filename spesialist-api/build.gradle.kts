val testcontainersVersion = "1.17.3"

dependencies {
    implementation(project(":spesialist-felles"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}
