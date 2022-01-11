import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

val graphQLKotlinVersion = "5.3.1"

plugins {
    kotlin("plugin.serialization") version "1.5.31"
    id("com.expediagroup.graphql") version "5.3.1"
}

dependencies {
    implementation("com.expediagroup:graphql-kotlin-server:$graphQLKotlinVersion")
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    testImplementation(project(":testkode"))
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.dev-fss-pub.nais.io/graphql")
    outputFile.set(File("${project.projectDir}/src/main/resources/graphql/schema.graphql"))
}

// Disabler automatisk kjøring av introspection siden henting av schema feiler under bygg på Github.
// Kan kjøres manuelt for å hente nytt schema.
graphqlIntrospectSchema.enabled = false

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
