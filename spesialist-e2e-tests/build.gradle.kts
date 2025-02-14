dependencies {
    testImplementation(project(":spesialist-bootstrap"))

    testImplementation(libs.tbd.libs.rapids.and.rivers.test)
    testImplementation(libs.tbs.libs.jackson)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.graphql.kotlin.ktor.server)
}
