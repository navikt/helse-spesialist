import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateSDLTask

val testcontainersVersion = "1.17.3"
val graphQLKotlinVersion = "6.2.5"
val ktorVersion = "2.0.1"

plugins {
    kotlin("plugin.serialization") version "1.6.21"
    id("com.expediagroup.graphql") version "5.4.1"
}

dependencies {
    implementation(project(":spesialist-felles"))
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
}
val graphqlIntrospectSchema by tasks.getting(com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.dev.intern.nav.no/graphql")
    outputFile.set(File("${project.projectDir}/src/main/resources/graphql/schema.graphql"))
}

// Disabler automatisk kjøring av introspection siden henting av schema feiler under bygg på Github.
// Kan kjøres manuelt for å hente nytt schema.
graphqlIntrospectSchema.enabled = System.getenv("CI") != "true"

val graphqlGenerateClient by tasks.getting(com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask::class) {
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

val graphqlGenerateSDL by tasks.getting(GraphQLGenerateSDLTask::class) {
    packages.set(listOf(
        "no.nav.helse.spesialist.api.graphql.schema",
        "no.nav.helse.spesialist.api.graphql.mutation",
        "no.nav.helse.spesialist.api.graphql.query",
    ))
}

graphqlGenerateSDL.enabled = false
