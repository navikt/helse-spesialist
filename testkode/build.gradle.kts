val junitJupiterVersion = "5.7.2"
val testcontainersVersion = "1.16.2"
val ktorVersion = "1.6.7"

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    implementation("io.mockk:mockk:1.12.0")

    implementation("org.testcontainers:postgresql:$testcontainersVersion")
}
