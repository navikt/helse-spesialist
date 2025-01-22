dependencies {
    implementation(project(":spesialist-selve"))
    implementation(project(":spesialist-db-migrations"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(libs.testcontainers.postgresql)
}
