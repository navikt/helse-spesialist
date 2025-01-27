val micrometerRegistryPrometheusVersion = "1.13.6"

dependencies {
    implementation(project(":spesialist-db-migrations"))
    implementation(project(":spesialist-selve"))

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(libs.testcontainers.postgresql)
}
