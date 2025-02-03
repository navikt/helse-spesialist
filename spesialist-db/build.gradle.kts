val micrometerRegistryPrometheusVersion = "1.14.3"

dependencies {
    implementation(project(":spesialist-db-migrations"))
    implementation(project(":spesialist-selve"))
    implementation(project(":spesialist-modell"))
    implementation(libs.bundles.db)

    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(libs.testcontainers.postgresql)
}
