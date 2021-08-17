val junitJupiterVersion = "5.7.2"

dependencies {
    implementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    implementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    implementation("com.opentable.components:otj-pg-embedded:0.13.4")
    implementation("io.mockk:mockk:1.12.0")
}
