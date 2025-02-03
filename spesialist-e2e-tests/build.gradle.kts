val mockOAuth2ServerVersion = "2.1.10"

dependencies {
    testImplementation(project(":spesialist-api"))
    testImplementation(project(":spesialist-api-schema"))
    testImplementation(project(":spesialist-bootstrap"))
    testImplementation(project(":spesialist-db"))
    testImplementation(project(":spesialist-modell"))
    testImplementation(project(":spesialist-kafka"))
    testImplementation(project(":spesialist-selve"))

    testImplementation(libs.rapids.and.rivers)
    testImplementation(libs.rapids.and.rivers.test)

    testImplementation(libs.jackson.helpers)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.bundles.ktor.client)
    testImplementation(libs.bundles.ktor.server)
    testImplementation(libs.bundles.ktor.server.test)
    testImplementation(libs.graphql.kotlin.ktor.server)
    testImplementation(libs.jackson.datatype)
    testImplementation(libs.bundles.db)
    testImplementation(libs.ktor.server.auth.jwt)
}
