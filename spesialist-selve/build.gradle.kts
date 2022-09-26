import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateSDLTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

val graphQLKotlinVersion = "5.5.0"
val testcontainersVersion = "1.17.3"
val ktorVersion = "2.1.1"

plugins {
    kotlin("plugin.serialization") version "1.6.21"
    id("com.expediagroup.graphql") version "5.4.1"
}

dependencies {
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.dev.intern.nav.no/graphql")
    outputFile.set(File("${project.projectDir}/src/main/resources/graphql/schema.graphql"))
}

// Disabler automatisk kjøring av introspection siden henting av schema feiler under bygg på Github.
// Kan kjøres manuelt for å hente nytt schema.
graphqlIntrospectSchema.enabled = System.getenv("CI") != "true"

val graphqlGenerateClient by tasks.getting(GraphQLGenerateClientTask::class) {
    val baseDir = "${project.projectDir}/src/main/resources/graphql"

    serializer.set(JACKSON)
    packageName.set("no.nav.helse.mediator.graphql")
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
    packages.set(
        listOf(
            "no.nav.helse.mediator.api.graphql",
            "no.nav.helse.mediator.api.graphql.schema",
            "no.nav.helse.mediator.graphql"
        )
    )
}

graphqlGenerateSDL.enabled = false

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
