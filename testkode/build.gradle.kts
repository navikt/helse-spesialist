val junitJupiterVersion = "5.7.2"
val testcontainersVersion = "1.16.2"

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    implementation("io.mockk:mockk:1.12.0")

    implementation("org.testcontainers:postgresql:$testcontainersVersion")
}
