import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask

val testcontainersVersion = "1.18.3"
val graphQLKotlinVersion = "6.4.1"
val ktorVersion = "2.3.1"

plugins {
    kotlin("plugin.serialization") version "1.8.21"
    id("com.expediagroup.graphql") version "6.4.1"
}

dependencies {
    api("com.nimbusds:nimbus-jose-jwt:9.31")
    api("io.ktor:ktor-server-double-receive:$ktorVersion")
    implementation(project(":spesialist-felles"))
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
}

val graphqlIntrospectSchema by tasks.getting(com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.intern.dev.nav.no/v2/graphql/introspection")
    outputFile.set(File("${project.projectDir}/src/main/resources/graphql/schema.graphql"))
}

// Disabler automatisk kjøring av introspection siden henting av schema feiler under bygg på Github.
// Kan kjøres manuelt for å hente nytt schema.
graphqlIntrospectSchema.enabled = System.getenv("CI") != "true"

val graphqlGenerateClient by tasks.getting(GraphQLGenerateClientTask::class) {
    val baseDir = "${project.projectDir}/src/main/resources/graphql"

    serializer.set(com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON)
    packageName.set("no.nav.helse.spesialist.api.graphql")
    schemaFile.set(graphqlIntrospectSchema.outputFile)
    queryFiles.from(
        listOf(
            File("$baseDir/hentSnapshot.graphql")
        )
    )

    if (graphqlIntrospectSchema.enabled) {
        dependsOn("graphqlIntrospectSchema")
    }
}

tasks.processResources {
    dependsOn(tasks.graphqlIntrospectSchema)
}
