val junitJupiterVersion = "5.7.1"

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    implementation("com.opentable.components:otj-pg-embedded:0.13.3") //Oktober, 2019
    implementation("io.mockk:mockk:1.10.6")

    implementation("org.testcontainers:postgresql:1.15.1")

}
